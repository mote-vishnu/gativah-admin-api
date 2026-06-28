package com.gativah.admin.moderation.query;

import java.util.List;

import com.gativah.admin.moderation.dto.ReportDetail;
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
    // ("could not determine data type of parameter $1").
    private static final String FILTER = """
            where (cast(:status as varchar) is null or cr.status = :status)
              and (cast(:contentType as varchar) is null or cr.content_type = :contentType)
              and (cast(:reason as varchar) is null or cr.reason = :reason)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ModerationQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<ReportSummary> queue(String status, String contentType, String reason, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("contentType", contentType)
                .addValue("reason", reason);

        Long total = jdbc.queryForObject("select count(*) " + JOINS + FILTER, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select cr.id, cr.content_type, cr.content_id, cr.reason, cr.status, cr.created_at, "
                + "cr.reporter_user_id, ru.username as reporter_username, "
                + "coalesce(p.author_user_id, c.author_user_id) as author_user_id, au.username as author_username, "
                + "left(coalesce(p.content, c.content), 160) as snippet, cr.assignee_admin_id "
                + JOINS + FILTER
                + " order by cr.created_at desc limit :limit offset :offset";
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
                (Long) rs.getObject("assignee_admin_id")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public ReportDetail detail(Long reportId) {
        String sql = "select cr.id, cr.content_type, cr.content_id, cr.reason, cr.details, cr.status, cr.created_at, "
                + "cr.reporter_user_id, ru.username as reporter_username, "
                + "coalesce(p.author_user_id, c.author_user_id) as author_user_id, au.username as author_username, "
                + "au.account_status as author_status, "
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
                    (Long) rs.getObject("author_user_id"),
                    rs.getString("author_username"),
                    rs.getString("author_status"),
                    rs.getString("snippet"),
                    (Long) rs.getObject("reviewed_by"),
                    rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toLocalDateTime(),
                    (Long) rs.getObject("assignee_admin_id")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
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
