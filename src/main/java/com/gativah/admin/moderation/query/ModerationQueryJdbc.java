package com.gativah.admin.moderation.query;

import java.math.BigDecimal;
import java.util.List;

import com.gativah.admin.moderation.dto.AppealRow;
import com.gativah.admin.moderation.dto.AuthorSanction;
import com.gativah.admin.moderation.dto.AuthorStats;
import com.gativah.admin.moderation.dto.AutoFlagSignal;
import com.gativah.admin.moderation.dto.ReasonCount;
import com.gativah.admin.moderation.dto.RegionBanRow;
import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.ReportStats;
import com.gativah.admin.moderation.dto.ReportSummary;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL read side for the grievance queue. content_report references
 * content polymorphically by (content_type, content_id), so the author + snippet
 * are resolved with conditional left-joins to post / post_comment. All filtering
 * + paging happens in the database.
 */
@Repository
public class ModerationQueryJdbc implements ModerationQuery {

    private static final String JOINS = """
            from content_report cr
            left join user_account ru on ru.id = cr.reporter_user_id
            left join post p on cr.content_type = 'POST' and p.id = cr.content_id
            left join post_comment c on cr.content_type = 'COMMENT' and c.id = cr.content_id
            left join user_account au on au.id = coalesce(p.author_user_id, c.author_user_id)
            """;

    // Each named param expands to a distinct positional placeholder, so the
    // standalone "is null" check must be cast or Postgres can't infer its type
    // ("could not determine data type of parameter $1"). A multi-value status
    // filter is appended as an in-clause only when values are supplied.
    private static final String FILTER = """
            where (cast(:contentType as varchar) is null or cr.content_type = :contentType)
              and (cast(:reason as varchar) is null or cr.reason = :reason)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ModerationQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Whitelisted ORDER BY — the sort key arrives from the client, so it is mapped
    // to a known column and never interpolated raw.
    private static String orderClause(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String col = switch (order.getProperty()) {
                case "status" -> "cr.status";
                case "reason" -> "cr.reason";
                case "contentType" -> "cr.content_type";
                default -> "cr.created_at";
            };
            return col + (order.isAscending() ? " asc" : " desc");
        }
        return "cr.created_at desc";
    }

    @Override
    public Page<ReportSummary> queue(List<String> statuses, String contentType, String reason, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("contentType", contentType)
                .addValue("reason", reason);
        String filter = FILTER;
        if (statuses != null && !statuses.isEmpty()) {
            filter = filter + " and cr.status in (:statuses)";
            params.addValue("statuses", statuses);
        }

        Long total = jdbc.queryForObject("select count(*) " + JOINS + filter, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select cr.id, cr.content_type, cr.content_id, cr.reason, cr.status, cr.created_at, "
                + "cr.reporter_user_id, ru.username as reporter_username, "
                + "coalesce(p.author_user_id, c.author_user_id) as author_user_id, au.username as author_username, "
                + "left(coalesce(p.content, c.content), 160) as snippet, cr.assignee_admin_id, "
                + "(select max(case s.severity when 'HIGH' then 3 when 'MED' then 2 else 1 end) "
                + "   from content_report_signal s where s.report_id = cr.id) as max_sev, "
                + "(select count(*) from content_report cr2 where cr2.content_type = cr.content_type "
                + "   and cr2.content_id = cr.content_id) as reporter_count, "
                + "(select count(*) from content_report cr3 "
                + "   left join post p3 on cr3.content_type = 'POST' and p3.id = cr3.content_id "
                + "   left join post_comment c3 on cr3.content_type = 'COMMENT' and c3.id = cr3.content_id "
                + "   where coalesce(p3.author_user_id, c3.author_user_id) = coalesce(p.author_user_id, c.author_user_id) "
                + "     and cr3.status in ('PENDING', 'REVIEWING')) as open_on_author "
                + JOINS + filter
                + " order by " + orderClause(pageable) + " limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<ReportSummary> rows = jdbc.query(sql, params, (rs, i) -> new ReportSummary(
                rs.getLong("id"),
                rs.getString("content_type"),
                (Long) rs.getObject("content_id"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                (Long) rs.getObject("reporter_user_id"),
                rs.getString("reporter_username"),
                (Long) rs.getObject("author_user_id"),
                rs.getString("author_username"),
                rs.getString("snippet"),
                (Long) rs.getObject("assignee_admin_id"),
                severityLabel(rs.getObject("max_sev") == null ? 0 : rs.getInt("max_sev")),
                rs.getLong("reporter_count"),
                rs.getLong("open_on_author")));
        return new PageImpl<>(rows, pageable, count);
    }

    private static String severityLabel(int rank) {
        return switch (rank) {
            case 3 -> "HIGH";
            case 2 -> "MED";
            case 1 -> "LOW";
            default -> null;
        };
    }

    @Override
    public ReportDetail detail(Long reportId) {
        String sql = "select cr.id, cr.content_type, cr.content_id, cr.reason, cr.details, cr.status, cr.created_at, "
                + "cr.reporter_user_id, ru.username as reporter_username, "
                + "(select count(*) from content_report cr2 where cr2.content_type = cr.content_type "
                + "   and cr2.content_id = cr.content_id) as reporter_count, "
                + "coalesce(p.author_user_id, c.author_user_id) as author_user_id, au.username as author_username, "
                + "trim(concat(au.first_name, ' ', au.last_name)) as author_display_name, au.photo_url as author_photo_url, "
                + "au.account_status as author_status, "
                + "case when cr.content_type = 'POST' then p.privacy else null end as privacy, "
                + "(select count(*) from post_media pm where cr.content_type = 'POST' and pm.post_id = cr.content_id) as media_count, "
                + "coalesce(p.content, c.content) as snippet, cr.reviewed_by, cr.reviewed_at, cr.assignee_admin_id "
                + JOINS + " where cr.id = :id";
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource("id", reportId), (rs, i) -> new ReportDetail(
                    rs.getLong("id"),
                    rs.getString("content_type"),
                    (Long) rs.getObject("content_id"),
                    rs.getString("reason"),
                    rs.getString("details"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                    (Long) rs.getObject("reporter_user_id"),
                    rs.getString("reporter_username"),
                    rs.getLong("reporter_count"),
                    (Long) rs.getObject("author_user_id"),
                    rs.getString("author_username"),
                    rs.getString("author_display_name"),
                    rs.getString("author_photo_url"),
                    rs.getString("author_status"),
                    rs.getString("privacy"),
                    rs.getInt("media_count"),
                    rs.getString("snippet"),
                    (Long) rs.getObject("reviewed_by"),
                    rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toLocalDateTime(),
                    (Long) rs.getObject("assignee_admin_id")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<ReasonCount> queueByReason() {
        String sql = "select reason, count(*) as cnt from content_report "
                + "where status in ('PENDING', 'REVIEWING') group by reason order by cnt desc";
        return jdbc.query(sql, new MapSqlParameterSource(),
                (rs, i) -> new ReasonCount(rs.getString("reason"), rs.getLong("cnt")));
    }

    @Override
    public Page<AppealRow> appeals(String status, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("status", status);
        Long total = jdbc.queryForObject(
                "select count(*) from appeal where (cast(:status as varchar) is null or status = :status)", params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        String sql = "select a.id, a.subject_user_id, u.username as subject_username, a.related_report_id, "
                + "a.related_action_id, ma.action as original_action, a.message, a.status, a.created_at "
                + "from appeal a "
                + "left join user_account u on u.id = a.subject_user_id "
                + "left join moderation_action ma on ma.id = a.related_action_id "
                + "where (cast(:status as varchar) is null or a.status = :status) "
                + "order by a.created_at desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<AppealRow> rows = jdbc.query(sql, params, (rs, i) -> new AppealRow(
                rs.getLong("id"),
                (Long) rs.getObject("subject_user_id"),
                rs.getString("subject_username"),
                (Long) rs.getObject("related_report_id"),
                (Long) rs.getObject("related_action_id"),
                rs.getString("original_action"),
                rs.getString("message"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public List<RegionBanRow> regionBans() {
        String sql = "select rb.id, rb.post_id, rb.country, rb.reason, rb.banned_by_user_id, rb.banned_at, "
                + "rb.lifted_at, au.username as author_username, left(p.content, 120) as snippet "
                + "from post_region_ban rb "
                + "left join post p on p.id = rb.post_id "
                + "left join user_account au on au.id = p.author_user_id "
                + "order by (rb.lifted_at is null) desc, rb.banned_at desc limit 200";
        return jdbc.query(sql, new MapSqlParameterSource(), (rs, i) -> new RegionBanRow(
                rs.getLong("id"),
                (Long) rs.getObject("post_id"),
                rs.getString("country"),
                rs.getString("reason"),
                (Long) rs.getObject("banned_by_user_id"),
                rs.getTimestamp("banned_at") == null ? null : rs.getTimestamp("banned_at").toLocalDateTime(),
                rs.getObject("lifted_at") != null,
                rs.getString("author_username"),
                rs.getString("snippet")));
    }

    @Override
    public ReportStats stats() {
        // Per-reason SLA mirrors the client (Illegal 2h, Harassment/Nudity 4h, Misinformation 12h, else 24h).
        String agg = "select "
                + "count(*) filter (where status = 'PENDING') as pending, "
                + "count(*) filter (where status = 'REVIEWING') as reviewing, "
                + "count(*) filter (where status in ('RESOLVED','DISMISSED') and reviewed_at >= now() - interval '24 hours') as resolved24h, "
                + "count(*) filter (where status in ('PENDING','REVIEWING') and created_at < now() - "
                + "   (case reason when 'Illegal' then interval '2 hours' when 'Harassment' then interval '4 hours' "
                + "                when 'Nudity' then interval '4 hours' when 'Misinformation' then interval '12 hours' "
                + "                else interval '24 hours' end)) as sla_breaches "
                + "from content_report";
        MapSqlParameterSource noParams = new MapSqlParameterSource();
        long[] counts = jdbc.queryForObject(agg, noParams, (rs, i) -> new long[] {
                rs.getLong("pending"), rs.getLong("reviewing"), rs.getLong("resolved24h"), rs.getLong("sla_breaches") });

        String repeat = "select count(*) from ("
                + "select coalesce(p.author_user_id, c.author_user_id) as author "
                + "from content_report cr "
                + "left join post p on cr.content_type = 'POST' and p.id = cr.content_id "
                + "left join post_comment c on cr.content_type = 'COMMENT' and c.id = cr.content_id "
                + "where cr.status in ('PENDING','REVIEWING') "
                + "group by author having count(*) >= 3 and coalesce(p.author_user_id, c.author_user_id) is not null) t";
        Long repeatOffenders = jdbc.queryForObject(repeat, noParams, Long.class);

        long pending = counts == null ? 0 : counts[0];
        long reviewing = counts == null ? 0 : counts[1];
        return new ReportStats(pending + reviewing, pending, reviewing,
                counts == null ? 0 : counts[3], counts == null ? 0 : counts[2],
                repeatOffenders == null ? 0 : repeatOffenders);
    }

    @Override
    public long reportsAgainst(Long authorUserId) {
        String sql = "select count(*) from content_report cr "
                + "left join post p on cr.content_type = 'POST' and p.id = cr.content_id "
                + "left join post_comment c on cr.content_type = 'COMMENT' and c.id = cr.content_id "
                + "where coalesce(p.author_user_id, c.author_user_id) = :authorId";
        Long n = jdbc.queryForObject(sql, new MapSqlParameterSource("authorId", authorUserId), Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public List<AuthorSanction> recentSanctions(Long authorUserId, int limit) {
        String sql = "select type, reason, suspended_until, created_at from user_sanction "
                + "where user_id = :authorId order by created_at desc limit :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("authorId", authorUserId).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, i) -> new AuthorSanction(
                rs.getString("type"),
                rs.getString("reason"),
                rs.getTimestamp("suspended_until") == null ? null : rs.getTimestamp("suspended_until").toLocalDateTime(),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()));
    }

    @Override
    public AuthorStats authorStats(Long authorUserId) {
        String reportsJoin = "from content_report cr "
                + "left join post p on cr.content_type = 'POST' and p.id = cr.content_id "
                + "left join post_comment c on cr.content_type = 'COMMENT' and c.id = cr.content_id "
                + "where coalesce(p.author_user_id, c.author_user_id) = :id";
        String sql = "select au.account_status, au.created_at as member_since, "
                + "(select count(*) from follow f where f.followed_user_id = :id and f.status = 'ACCEPTED') as followers, "
                + "(select count(*) " + reportsJoin + ") as reports_against, "
                + "(select count(*) " + reportsJoin + " and cr.status in ('PENDING', 'REVIEWING')) as open_reports, "
                + "(select case when bool_or(entitlement_code = 'verified') then 'Verified' "
                + "             when bool_or(entitlement_code like 'plus%') then 'Plus' else 'Free' end "
                + "   from user_entitlement ue where ue.user_id = :id and ue.active = true) as plan "
                + "from user_account au where au.id = :id";
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource("id", authorUserId), (rs, i) -> new AuthorStats(
                    rs.getString("account_status"),
                    rs.getLong("reports_against"),
                    rs.getLong("open_reports"),
                    rs.getLong("followers"),
                    rs.getString("plan") == null ? "Free" : rs.getString("plan"),
                    rs.getTimestamp("member_since") == null ? null : rs.getTimestamp("member_since").toLocalDateTime()));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<AutoFlagSignal> signals(Long reportId) {
        String sql = "select signal_key, label, score, is_boolean, bool_value, severity "
                + "from content_report_signal where report_id = :id "
                + "order by case severity when 'HIGH' then 0 when 'MED' then 1 else 2 end, id";
        return jdbc.query(sql, new MapSqlParameterSource("id", reportId), (rs, i) -> new AutoFlagSignal(
                rs.getString("signal_key"),
                rs.getString("label"),
                (BigDecimal) rs.getObject("score"),
                rs.getBoolean("is_boolean"),
                (Boolean) rs.getObject("bool_value"),
                rs.getString("severity")));
    }

    @Override
    public Long authorOf(String contentType, Long contentId) {
        String table = "POST".equalsIgnoreCase(contentType) ? "post"
                : "COMMENT".equalsIgnoreCase(contentType) ? "post_comment" : null;
        if (table == null) {
            return null;
        }
        String sql = "select author_user_id from " + table + " where id = :id";
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource("id", contentId), Long.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
