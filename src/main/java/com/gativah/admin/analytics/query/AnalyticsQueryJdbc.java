package com.gativah.admin.analytics.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gativah.admin.analytics.dto.CohortSize;
import com.gativah.admin.analytics.dto.CountryCount;
import com.gativah.admin.analytics.dto.EventBreakdownRow;
import com.gativah.admin.analytics.dto.PlatformRow;
import com.gativah.admin.analytics.dto.RetentionCell;
import com.gativah.admin.analytics.dto.StickinessPoint;
import com.gativah.admin.analytics.dto.TimePoint;
import com.gativah.admin.analytics.dto.VersionRow;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native-SQL aggregation over analytics_event (V80) + user_account. */
@Repository
public class AnalyticsQueryJdbc implements AnalyticsQuery {

    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long distinctActiveUsers(LocalDateTime from, LocalDateTime to) {
        return scalar("select count(distinct user_id) from analytics_event where received_at >= :from and received_at < :to",
                new MapSqlParameterSource().addValue("from", from).addValue("to", to));
    }

    @Override
    public long countEvents(LocalDateTime from, LocalDateTime to) {
        return scalar("select count(*) from analytics_event where received_at >= :from and received_at < :to",
                new MapSqlParameterSource().addValue("from", from).addValue("to", to));
    }

    @Override
    public long countSignups(LocalDateTime from, LocalDateTime to) {
        return scalar("select count(*) from user_account where created_at >= :from and created_at < :to",
                new MapSqlParameterSource().addValue("from", from).addValue("to", to));
    }

    @Override
    public List<TimePoint> activeUsersByDay(LocalDate from, LocalDate to) {
        String sql = """
                select g.d::date as day, coalesce(a.cnt, 0) as value
                from generate_series(:from, :to, interval '1 day') g(d)
                left join (
                    select date_trunc('day', received_at)::date as dd, count(distinct user_id) as cnt
                    from analytics_event
                    where received_at >= :from and received_at < (:to::date + interval '1 day')
                    group by 1
                ) a on a.dd = g.d::date
                order by day
                """;
        return jdbc.query(sql, range(from, to),
                (rs, i) -> new TimePoint(rs.getObject("day", LocalDate.class), rs.getLong("value")));
    }

    @Override
    public List<TimePoint> signupsByDay(LocalDate from, LocalDate to) {
        String sql = """
                select g.d::date as day, coalesce(a.cnt, 0) as value
                from generate_series(:from, :to, interval '1 day') g(d)
                left join (
                    select date_trunc('day', created_at)::date as dd, count(*) as cnt
                    from user_account
                    where created_at >= :from and created_at < (:to::date + interval '1 day')
                    group by 1
                ) a on a.dd = g.d::date
                order by day
                """;
        return jdbc.query(sql, range(from, to),
                (rs, i) -> new TimePoint(rs.getObject("day", LocalDate.class), rs.getLong("value")));
    }

    @Override
    public List<EventBreakdownRow> eventBreakdown(LocalDateTime from, LocalDateTime to) {
        String sql = """
                select name, count(*) as cnt, count(distinct user_id) as users
                from analytics_event
                where received_at >= :from and received_at < :to
                group by name
                order by cnt desc
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("from", from).addValue("to", to),
                (rs, i) -> new EventBreakdownRow(rs.getString("name"), rs.getLong("cnt"), rs.getLong("users"), 0));
    }

    @Override
    public List<StickinessPoint> stickinessByDay(LocalDate from, LocalDate to) {
        String sql = """
                select g.d::date as day,
                    (select count(distinct e.user_id) from analytics_event e
                     where e.received_at >= g.d and e.received_at < g.d + interval '1 day') as dau,
                    (select count(distinct e.user_id) from analytics_event e
                     where e.received_at >= g.d - interval '29 days' and e.received_at < g.d + interval '1 day') as mau
                from generate_series(:from, :to, interval '1 day') g(d)
                order by day
                """;
        return jdbc.query(sql, range(from, to), (rs, i) -> {
            long dau = rs.getLong("dau");
            long mau = rs.getLong("mau");
            double stickiness = mau > 0 ? Math.round(dau * 1000.0 / mau) / 10.0 : 0;
            return new StickinessPoint(rs.getObject("day", LocalDate.class), dau, mau, stickiness);
        });
    }

    @Override
    public List<PlatformRow> platformBreakdown(LocalDateTime from, LocalDateTime to) {
        String sql = """
                select coalesce(platform, 'unknown') as platform, count(*) as events, count(distinct user_id) as users
                from analytics_event
                where received_at >= :from and received_at < :to
                group by 1
                order by events desc
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("from", from).addValue("to", to),
                (rs, i) -> new PlatformRow(rs.getString("platform"), rs.getLong("events"), rs.getLong("users"), 0));
    }

    @Override
    public List<VersionRow> versionBreakdown(LocalDateTime from, LocalDateTime to) {
        String sql = """
                select coalesce(app_version, 'unknown') as v, count(*) as events, count(distinct user_id) as users
                from analytics_event
                where received_at >= :from and received_at < :to
                group by 1
                order by events desc
                limit 12
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("from", from).addValue("to", to),
                (rs, i) -> new VersionRow(rs.getString("v"), rs.getLong("events"), rs.getLong("users")));
    }

    @Override
    public Map<String, Long> funnelUsers(LocalDateTime from, LocalDateTime to, List<String> events) {
        String sql = """
                select name, count(distinct user_id) as users
                from analytics_event
                where received_at >= :from and received_at < :to and name in (:events)
                group by name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from).addValue("to", to).addValue("events", events);
        Map<String, Long> out = new HashMap<>();
        jdbc.query(sql, params, (rs) -> { out.put(rs.getString("name"), rs.getLong("users")); });
        return out;
    }

    @Override
    public List<CohortSize> cohortSizes(LocalDateTime from) {
        String sql = """
                select date_trunc('week', created_at)::date as cohort_week, count(*) as size
                from user_account
                where created_at >= :from
                group by 1
                order by 1
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("from", from),
                (rs, i) -> new CohortSize(rs.getObject("cohort_week", LocalDate.class), rs.getLong("size")));
    }

    @Override
    public List<RetentionCell> retentionCells(LocalDateTime from) {
        String sql = """
                with cohorts as (
                    select id as user_id, date_trunc('week', created_at)::date as cohort_week
                    from user_account
                    where created_at >= :from
                ),
                activity as (
                    select distinct user_id, date_trunc('week', received_at)::date as active_week
                    from analytics_event
                    where received_at >= :from
                )
                select c.cohort_week,
                    ((a.active_week - c.cohort_week) / 7)::int as week_offset,
                    count(distinct c.user_id) as retained
                from cohorts c
                join activity a on a.user_id = c.user_id and a.active_week >= c.cohort_week
                group by c.cohort_week, week_offset
                order by c.cohort_week, week_offset
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("from", from),
                (rs, i) -> new RetentionCell(rs.getObject("cohort_week", LocalDate.class),
                        rs.getInt("week_offset"), rs.getLong("retained")));
    }

    @Override
    public List<CountryCount> geoByCountry() {
        String sql = """
                select country, count(*) as users from (
                    select ua.id, (
                        select bt.country_code from billing_transaction bt
                        where bt.user_id = ua.id and bt.country_code is not null
                        order by bt.created_at desc limit 1
                    ) as country
                    from user_account ua
                ) uc
                where country is not null
                group by country
                order by users desc
                """;
        return jdbc.query(sql, new MapSqlParameterSource(),
                (rs, i) -> new CountryCount(rs.getString("country"), rs.getLong("users")));
    }

    @Override
    public long totalUsers() {
        Long n = jdbc.queryForObject("select count(*) from user_account", new MapSqlParameterSource(), Long.class);
        return n == null ? 0 : n;
    }

    private long scalar(String sql, MapSqlParameterSource params) {
        Long v = jdbc.queryForObject(sql, params, Long.class);
        return v == null ? 0 : v;
    }

    private static MapSqlParameterSource range(LocalDate from, LocalDate to) {
        return new MapSqlParameterSource().addValue("from", from).addValue("to", to);
    }
}
