package com.gativah.admin.clubs.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubEventRow;
import com.gativah.admin.clubs.dto.ClubMemberRow;
import com.gativah.admin.clubs.dto.ClubSummary;

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
                    members,
                    events));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
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
