package com.gativah.admin.clubs.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubEventDetail;
import com.gativah.admin.clubs.dto.ClubEventRow;
import com.gativah.admin.clubs.dto.ClubInsights;
import com.gativah.admin.clubs.dto.ClubMemberRow;
import com.gativah.admin.clubs.dto.ClubReportedContent;
import com.gativah.admin.clubs.dto.ClubStats;
import com.gativah.admin.clubs.dto.ClubSummary;
import com.gativah.admin.clubs.dto.RoutePoint;
import com.gativah.admin.clubs.dto.RsvpRow;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native-SQL read side for the Clubs directory. */
@Repository
public class ClubQueryJdbc implements ClubQuery {

    // The name-clause is static (cast for type inference); visibility + removed
    // multi-value filters are appended as in-clauses only when values are supplied.
    private static final String FILTER = """
            where (cast(:q as varchar) is null or c.name ilike :q)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ClubQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String orderClause(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String col = switch (order.getProperty()) {
                case "name" -> "c.name";
                case "members" -> "c.member_count";
                case "events" -> "event_count";
                case "visibility" -> "c.visibility";
                default -> "c.created_at";
            };
            return col + (order.isAscending() ? " asc" : " desc");
        }
        return "c.created_at desc";
    }

    @Override
    public Page<ClubSummary> search(String q, List<String> visibilities, List<Boolean> removed, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("q", q);
        String filter = FILTER;
        if (visibilities != null && !visibilities.isEmpty()) {
            filter = filter + " and c.visibility in (:visibilities)";
            params.addValue("visibilities", visibilities);
        }
        if (removed != null && !removed.isEmpty()) {
            filter = filter + " and (c.deleted_at is not null) in (:removed)";
            params.addValue("removed", removed);
        }

        Long total = jdbc.queryForObject("select count(*) from club c " + filter, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select c.id, c.name, c.owner_user_id, au.username as owner_username, c.visibility, "
                + "c.member_count, (c.deleted_at is not null) as removed, c.created_at, "
                + "(select count(*) from club_event e where e.club_id = c.id and e.deleted_at is null) as event_count "
                + "from club c left join user_account au on au.id = c.owner_user_id " + filter
                + " order by " + orderClause(pageable) + " limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<ClubSummary> rows = jdbc.query(sql, params, (rs, i) -> new ClubSummary(
                rs.getLong("id"),
                rs.getString("name"),
                (Long) rs.getObject("owner_user_id"),
                rs.getString("owner_username"),
                rs.getString("visibility"),
                rs.getInt("member_count"),
                rs.getLong("event_count"),
                rs.getBoolean("removed"),
                ts(rs, "created_at")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public ClubDetail detail(Long id) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", id);
        String coreSql = "select c.id, c.name, c.description, c.photo_url, c.owner_user_id, "
                + "au.username as owner_username, c.visibility, c.member_count, "
                + "(c.deleted_at is not null) as removed, c.created_at "
                + "from club c left join user_account au on au.id = c.owner_user_id where c.id = :id";

        List<ClubMemberRow> members = members(id);
        List<ClubEventRow> events = events(id);
        ClubInsights insights = insights(id);

        try {
            return jdbc.queryForObject(coreSql, p, (rs, i) -> new ClubDetail(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("photo_url"),
                    (Long) rs.getObject("owner_user_id"),
                    rs.getString("owner_username"),
                    rs.getString("visibility"),
                    rs.getInt("member_count"),
                    rs.getBoolean("removed"),
                    ts(rs, "created_at"),
                    insights,
                    members,
                    events));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public ClubEventDetail eventDetail(Long clubId, Long eventId) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("cid", clubId).addValue("eid", eventId);
        String core = "select e.id, e.club_id, e.title, e.kind, e.description, e.location, e.starts_at, e.ends_at, "
                + "e.distance_m, e.created_by_user_id, u.username as created_by_username, e.created_at, "
                + "(e.deleted_at is not null) as removed, "
                + "(select count(*) from club_event_rsvp r where r.event_id = e.id and r.status = 'GOING') as going, "
                + "(select count(*) from club_event_rsvp r where r.event_id = e.id and r.status = 'MAYBE') as maybe, "
                + "(select count(*) from club_event_rsvp r where r.event_id = e.id and r.status = 'DECLINED') as declined "
                + "from club_event e left join user_account u on u.id = e.created_by_user_id "
                + "where e.id = :eid and e.club_id = :cid";

        List<ClubEventDetail> found = jdbc.query(core, p, (rs, i) -> new ClubEventDetail(
                rs.getLong("id"), (Long) rs.getObject("club_id"), rs.getString("title"), rs.getString("kind"),
                rs.getString("description"), rs.getString("location"), ts(rs, "starts_at"), ts(rs, "ends_at"),
                (Integer) rs.getObject("distance_m"), (Long) rs.getObject("created_by_user_id"),
                rs.getString("created_by_username"), ts(rs, "created_at"), rs.getBoolean("removed"),
                rs.getLong("going"), rs.getLong("maybe"), rs.getLong("declined"), List.of(), List.of()));
        if (found.isEmpty()) {
            return null;
        }
        ClubEventDetail e = found.get(0);

        List<RsvpRow> rsvps = jdbc.query(
                "select r.user_id, ru.username, r.status, r.updated_at from club_event_rsvp r "
                        + "left join user_account ru on ru.id = r.user_id where r.event_id = :eid "
                        + "order by case r.status when 'GOING' then 0 when 'MAYBE' then 1 else 2 end, r.updated_at desc "
                        + "limit 200",
                p, (rs, i) -> new RsvpRow((Long) rs.getObject("user_id"), rs.getString("username"),
                        rs.getString("status"), ts(rs, "updated_at")));

        List<RoutePoint> route = jdbc.query(
                "select seq_no, lat, lng from club_event_route_point where event_id = :eid order by seq_no",
                p, (rs, i) -> new RoutePoint(rs.getInt("seq_no"), rs.getDouble("lat"), rs.getDouble("lng")));

        return new ClubEventDetail(e.id(), e.clubId(), e.title(), e.kind(), e.description(), e.location(),
                e.startsAt(), e.endsAt(), e.distanceM(), e.createdByUserId(), e.createdByUsername(),
                e.createdAt(), e.removed(), e.rsvpGoing(), e.rsvpMaybe(), e.rsvpDeclined(), rsvps, route);
    }

    @Override
    public List<ClubReportedContent> reportedContent(Long clubId) {
        MapSqlParameterSource p = new MapSqlParameterSource("cid", clubId);
        // Union reported posts + reported comments that belong to this club, then aggregate per content.
        String sql = """
                select x.ctype, x.cid, max(x.snippet) as snippet, max(x.author_user_id) as author_user_id,
                       max(x.author_username) as author_username,
                       count(*) filter (where x.status in ('PENDING','REVIEWING')) as open_reports,
                       count(*) as total_reports,
                       max(x.report_id) as latest_report_id, max(x.report_at) as latest_report_at
                from (
                    select 'POST' as ctype, p.id as cid, left(p.content, 160) as snippet,
                           p.author_user_id, au.username as author_username,
                           cr.id as report_id, cr.status, cr.created_at as report_at
                    from content_report cr
                    join post p on p.id = cr.content_id and cr.content_type = 'POST' and p.club_id = :cid
                    left join user_account au on au.id = p.author_user_id
                    union all
                    select 'COMMENT' as ctype, pc.id as cid, left(pc.content, 160) as snippet,
                           pc.author_user_id, au.username as author_username,
                           cr.id as report_id, cr.status, cr.created_at as report_at
                    from content_report cr
                    join post_comment pc on pc.id = cr.content_id and cr.content_type = 'COMMENT'
                    join post p on p.id = pc.post_id and p.club_id = :cid
                    left join user_account au on au.id = pc.author_user_id
                ) x
                group by x.ctype, x.cid
                order by open_reports desc, total_reports desc, latest_report_at desc
                """;
        return jdbc.query(sql, p, (rs, i) -> new ClubReportedContent(
                rs.getString("ctype"), (Long) rs.getObject("cid"), rs.getString("snippet"),
                (Long) rs.getObject("author_user_id"), rs.getString("author_username"),
                rs.getLong("open_reports"), rs.getLong("total_reports"),
                (Long) rs.getObject("latest_report_id"), ts(rs, "latest_report_at")));
    }

    @Override
    public ClubStats stats() {
        String sql = """
                select
                  count(*) as total,
                  count(*) filter (where c.deleted_at is null) as active,
                  count(*) filter (where c.deleted_at is not null) as removed,
                  count(*) filter (where c.visibility = 'PRIVATE') as private,
                  coalesce(sum(c.member_count) filter (where c.deleted_at is null), 0) as members,
                  coalesce(avg(c.member_count) filter (where c.deleted_at is null), 0) as avg_members,
                  count(*) filter (where c.created_at >= now() - interval '30 days') as new_30d,
                  coalesce(max(c.member_count) filter (where c.deleted_at is null), 0) as largest,
                  (select count(*) from club_event e
                     join club cc on cc.id = e.club_id
                     where e.deleted_at is null and cc.deleted_at is null
                       and e.starts_at >= now()) as upcoming_events
                from club c
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), (rs, i) -> new ClubStats(
                rs.getLong("total"),
                rs.getLong("active"),
                rs.getLong("removed"),
                rs.getLong("private"),
                rs.getLong("members"),
                Math.round(rs.getDouble("avg_members") * 10.0) / 10.0,
                rs.getLong("new_30d"),
                rs.getLong("upcoming_events"),
                rs.getLong("largest")));
    }

    private ClubInsights insights(Long clubId) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", clubId);
        String sql = """
                select
                  (select count(*) from club_membership m where m.club_id = :id and m.role = 'OWNER') as owners,
                  (select count(*) from club_membership m where m.club_id = :id and m.role = 'ADMIN') as admins,
                  (select count(*) from club_membership m where m.club_id = :id and m.role not in ('OWNER','ADMIN')) as members,
                  (select count(*) from club_membership m where m.club_id = :id and m.status = 'PENDING') as pending,
                  (select count(*) from club_membership m where m.club_id = :id and m.joined_at >= now() - interval '30 days') as new_30d,
                  (select count(*) from club_event e where e.club_id = :id and e.deleted_at is null and e.starts_at >= now()) as upcoming,
                  (select count(*) from club_event e where e.club_id = :id and e.deleted_at is null and e.starts_at < now()) as past,
                  (select count(*) from club_event_rsvp r join club_event e on e.id = r.event_id
                     where e.club_id = :id and e.deleted_at is null) as rsvps
                """;
        return jdbc.queryForObject(sql, p, (rs, i) -> new ClubInsights(
                rs.getLong("owners"),
                rs.getLong("admins"),
                rs.getLong("members"),
                rs.getLong("pending"),
                rs.getLong("upcoming"),
                rs.getLong("past"),
                rs.getLong("rsvps"),
                rs.getLong("new_30d")));
    }

    private List<ClubMemberRow> members(Long clubId) {
        String sql = "select m.user_id, u.username, m.role, m.status, m.joined_at "
                + "from club_membership m left join user_account u on u.id = m.user_id "
                + "where m.club_id = :id order by m.joined_at desc limit 50";
        return jdbc.query(sql, new MapSqlParameterSource("id", clubId), (rs, i) -> new ClubMemberRow(
                (Long) rs.getObject("user_id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("status"),
                ts(rs, "joined_at")));
    }

    @Override
    public Page<ClubMemberRow> members(Long clubId, String role, String status, String q, Pageable pageable) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", clubId)
                .addValue("role", role)
                .addValue("status", status)
                .addValue("q", (q == null || q.isBlank()) ? null : "%" + q.trim() + "%");
        String where = "where m.club_id = :id "
                + "and (cast(:role as varchar) is null or m.role = :role) "
                + "and (cast(:status as varchar) is null or m.status = :status) "
                + "and (cast(:q as varchar) is null or u.username ilike :q) ";

        Long total = jdbc.queryForObject(
                "select count(*) from club_membership m left join user_account u on u.id = m.user_id " + where,
                p, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        // Owners/admins first, then newest members.
        String sql = "select m.user_id, u.username, m.role, m.status, m.joined_at "
                + "from club_membership m left join user_account u on u.id = m.user_id " + where
                + "order by case m.role when 'OWNER' then 0 when 'ADMIN' then 1 else 2 end, m.joined_at desc "
                + "limit :limit offset :offset";
        List<ClubMemberRow> rows = jdbc.query(sql, p, (rs, i) -> new ClubMemberRow(
                (Long) rs.getObject("user_id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("status"),
                ts(rs, "joined_at")));
        return new PageImpl<>(rows, pageable, count);
    }

    private List<ClubEventRow> events(Long clubId) {
        String sql = "select e.id, e.title, e.kind, e.starts_at, (e.deleted_at is not null) as removed, "
                + "(select count(*) from club_event_rsvp r where r.event_id = e.id) as rsvp_count "
                + "from club_event e where e.club_id = :id order by e.starts_at desc limit 50";
        return jdbc.query(sql, new MapSqlParameterSource("id", clubId), (rs, i) -> new ClubEventRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("kind"),
                ts(rs, "starts_at"),
                rs.getLong("rsvp_count"),
                rs.getBoolean("removed")));
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toLocalDateTime();
    }
}
