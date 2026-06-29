package com.gativah.admin.users.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.users.dto.ActivityPoint;
import com.gativah.admin.users.dto.DeviceRow;
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

    // Whitelisted ORDER BY — the sort key comes from the client, so it is mapped
    // to a known column (never interpolated raw) to stay injection-safe.
    private static String orderClause(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String col = switch (order.getProperty()) {
                case "username" -> "ua.username";
                case "email" -> "ua.email";
                case "status" -> "ua.account_status";
                case "verified" -> "ua.verified";
                default -> "ua.created_at";
            };
            return col + (order.isAscending() ? " asc" : " desc");
        }
        return "ua.created_at desc";
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
                + " order by " + orderClause(pageable) + " limit :limit offset :offset";
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

    @Override
    public String accountStatus(Long id) {
        List<String> rows = jdbc.query("select account_status from user_account where id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> rs.getString("account_status"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public long reportsAgainst(Long id) {
        String sql = "select count(*) from content_report cr "
                + "left join post p on cr.content_type = 'POST' and p.id = cr.content_id "
                + "left join post_comment c on cr.content_type = 'COMMENT' and c.id = cr.content_id "
                + "where coalesce(p.author_user_id, c.author_user_id) = :id";
        Long n = jdbc.queryForObject(sql, new MapSqlParameterSource("id", id), Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public long sanctionCount(Long id) {
        Long n = jdbc.queryForObject("select count(*) from user_sanction where user_id = :id",
                new MapSqlParameterSource("id", id), Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public List<DeviceRow> devices(Long id) {
        String sql = "select platform, app_version, locale, last_seen_at from device_token "
                + "where user_id = :id and deleted_at is null order by last_seen_at desc nulls last limit 20";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), (rs, i) -> new DeviceRow(
                rs.getString("platform"),
                rs.getString("app_version"),
                rs.getString("locale"),
                ts(rs, "last_seen_at")));
    }

    @Override
    public List<ActivityPoint> activity(Long id, int days) {
        String sql = "select activity_date, steps_count, active_minutes from daily_activity "
                + "where user_id = :id and activity_date >= current_date - cast(:days as integer) "
                + "order by activity_date asc";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id).addValue("days", days);
        return jdbc.query(sql, params, (rs, i) -> new ActivityPoint(
                rs.getObject("activity_date", LocalDate.class),
                rs.getLong("steps_count"),
                rs.getInt("active_minutes")));
    }

    private static String fullName(String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? null : name;
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toLocalDateTime();
    }
}
