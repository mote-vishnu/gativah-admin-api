package com.gativah.admin.users.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.users.dto.SanctionRow;
import com.gativah.admin.users.dto.SubscriptionInfo;
import com.gativah.admin.users.dto.UserDetail;
import com.gativah.admin.users.dto.UserSummary;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native-SQL read side for Users 360 over the shared consumer tables. */
@Repository
public class UsersQueryJdbc implements UsersQuery {

    // Each named param expands to a distinct placeholder; the standalone "is null"
    // check is cast so Postgres can infer its type. A multi-value status filter is
    // appended as an in-clause only when values are supplied.
    private static final String FILTER = """
            where (cast(:q as varchar) is null or ua.username ilike :q or ua.email ilike :q)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public UsersQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<UserSummary> search(String q, List<String> statuses, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("q", q);
        String filter = FILTER;
        if (statuses != null && !statuses.isEmpty()) {
            filter = filter + " and ua.account_status in (:statuses)";
            params.addValue("statuses", statuses);
        }

        Long total = jdbc.queryForObject("select count(*) from user_account ua " + filter, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select ua.id, ua.username, ua.email, ua.first_name, ua.last_name, ua.verified, "
                + "ua.account_status, ua.created_at, "
                + "(select s.state from subscription s where s.user_id = ua.id "
                + " order by s.current_period_end desc nulls last limit 1) as sub_state "
                + "from user_account ua " + filter
                + " order by ua.created_at desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<UserSummary> rows = jdbc.query(sql, params, (rs, i) -> new UserSummary(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                fullName(rs.getString("first_name"), rs.getString("last_name")),
                rs.getString("account_status"),
                rs.getBoolean("verified"),
                rs.getString("sub_state"),
                ts(rs, "created_at")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public UserDetail detail(Long id) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", id);
        String coreSql = "select ua.id, ua.username, ua.email, ua.first_name, ua.last_name, ua.photo_url, "
                + "ua.verified, ua.account_status, ua.suspended_until, ua.status_reason, ua.status_changed_at, ua.created_at "
                + "from user_account ua where ua.id = :id";

        SubscriptionInfo subscription = subscription(id);
        List<SanctionRow> sanctions = sanctions(id);

        try {
            return jdbc.queryForObject(coreSql, p, (rs, i) -> new UserDetail(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("photo_url"),
                    rs.getBoolean("verified"),
                    rs.getString("account_status"),
                    ts(rs, "suspended_until"),
                    rs.getString("status_reason"),
                    ts(rs, "status_changed_at"),
                    ts(rs, "created_at"),
                    subscription,
                    sanctions));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private SubscriptionInfo subscription(Long userId) {
        String sql = "select plan_code, platform, state, is_trial, auto_renew, current_period_end "
                + "from subscription where user_id = :id order by current_period_end desc nulls last limit 1";
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource("id", userId), (rs, i) -> new SubscriptionInfo(
                    rs.getString("plan_code"),
                    rs.getString("platform"),
                    rs.getString("state"),
                    rs.getBoolean("is_trial"),
                    rs.getBoolean("auto_renew"),
                    ts(rs, "current_period_end")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private List<SanctionRow> sanctions(Long userId) {
        String sql = "select id, type, reason, suspended_until, admin_user_id, created_at "
                + "from user_sanction where user_id = :id order by created_at desc";
        return jdbc.query(sql, new MapSqlParameterSource("id", userId), (rs, i) -> new SanctionRow(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("reason"),
                ts(rs, "suspended_until"),
                rs.getLong("admin_user_id"),
                ts(rs, "created_at")));
    }

    private static String fullName(String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? null : name;
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toLocalDateTime();
    }
}
