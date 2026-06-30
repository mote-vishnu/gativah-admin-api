package com.gativah.admin.legal.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.legal.dto.DisclosureRegisterRow;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.LegalStats;
import com.gativah.admin.legal.dto.LegalTaskListRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native-SQL aggregation over legal_request / legal_task / legal_disclosure. */
@Repository
public class LegalQueryJdbc implements LegalQuery {

    // A request still needs work and its due date has passed.
    private static final String OVERDUE_SQL =
            "status in ('RECEIVED','UNDER_REVIEW') and due_at is not null and due_at < now()";

    private final NamedParameterJdbcTemplate jdbc;

    public LegalQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<LegalRequestSummary> searchRequests(String q, List<String> statuses, List<String> types,
                                                    boolean overdueOnly, Pageable pageable) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("q", q == null || q.isBlank() ? null : "%" + q.trim() + "%");
        StringBuilder where = new StringBuilder("where (cast(:q as varchar) is null "
                + "or reference ilike :q or requesting_authority ilike :q) ");
        if (statuses != null && !statuses.isEmpty()) {
            where.append("and status in (:statuses) ");
            p.addValue("statuses", statuses);
        }
        if (types != null && !types.isEmpty()) {
            where.append("and request_type in (:types) ");
            p.addValue("types", types);
        }
        if (overdueOnly) {
            where.append("and ").append(OVERDUE_SQL).append(" ");
        }

        Long total = jdbc.queryForObject("select count(*) from legal_request " + where, p, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String sql = "select id, reference, request_type, requesting_authority, subject_user_id, status, "
                + "approval_status, received_at, due_at, (" + OVERDUE_SQL + ") as overdue, "
                + "(select count(*) from legal_disclosure d where d.request_id = legal_request.id) as disclosure_count "
                + "from legal_request " + where
                + "order by received_at desc limit :limit offset :offset";
        List<LegalRequestSummary> rows = jdbc.query(sql, p, (rs, i) -> new LegalRequestSummary(
                rs.getLong("id"), rs.getString("reference"), rs.getString("request_type"),
                rs.getString("requesting_authority"), (Long) rs.getObject("subject_user_id"),
                rs.getString("status"), rs.getString("approval_status"),
                ts(rs, "received_at"), ts(rs, "due_at"), rs.getBoolean("overdue"), rs.getLong("disclosure_count")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public LegalStats stats() {
        String sql = "select "
                + "count(*) filter (where status not in ('CLOSED','REJECTED')) as open_requests, "
                + "count(*) filter (where status = 'UNDER_REVIEW') as under_review, "
                + "count(*) filter (where approval_status = 'PENDING' and status not in ('CLOSED','REJECTED')) as pending_approval, "
                + "count(*) filter (where " + OVERDUE_SQL + ") as overdue, "
                + "count(*) filter (where status = 'ACTIONED' and updated_at >= now() - interval '30 days') as actioned_30d, "
                + "(select count(*) from legal_disclosure where disclosed_at >= now() - interval '30 days') as disclosures_30d, "
                + "(select count(*) from legal_task where status = 'OPEN') as open_tasks "
                + "from legal_request";
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), (rs, i) -> new LegalStats(
                rs.getLong("open_requests"), rs.getLong("under_review"), rs.getLong("pending_approval"),
                rs.getLong("overdue"), rs.getLong("actioned_30d"), rs.getLong("disclosures_30d"),
                rs.getLong("open_tasks")));
    }

    @Override
    public Page<LegalTaskListRow> openTasks(Pageable pageable) {
        Long total = jdbc.queryForObject("select count(*) from legal_task where status = 'OPEN'",
                new MapSqlParameterSource(), Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String sql = "select t.id, t.request_id, r.reference, r.request_type, t.title, t.status, "
                + "t.assignee_admin_id, t.due_at, t.created_at, "
                + "(t.due_at is not null and t.due_at < now()) as overdue "
                + "from legal_task t join legal_request r on r.id = t.request_id "
                + "where t.status = 'OPEN' "
                + "order by (t.due_at is not null and t.due_at < now()) desc, t.due_at asc nulls last, t.created_at desc "
                + "limit :limit offset :offset";
        List<LegalTaskListRow> rows = jdbc.query(sql, p, (rs, i) -> new LegalTaskListRow(
                rs.getLong("id"), rs.getLong("request_id"), rs.getString("reference"), rs.getString("request_type"),
                rs.getString("title"), rs.getString("status"), (Long) rs.getObject("assignee_admin_id"),
                ts(rs, "due_at"), ts(rs, "created_at"), rs.getBoolean("overdue")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public Page<DisclosureRegisterRow> disclosureRegister(Pageable pageable) {
        Long total = jdbc.queryForObject("select count(*) from legal_disclosure",
                new MapSqlParameterSource(), Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String sql = "select d.id, d.request_id, r.reference, r.request_type, d.disclosed_by, d.recipient, "
                + "d.data_categories, d.justification, d.disclosed_at "
                + "from legal_disclosure d join legal_request r on r.id = d.request_id "
                + "order by d.disclosed_at desc limit :limit offset :offset";
        List<DisclosureRegisterRow> rows = jdbc.query(sql, p, (rs, i) -> new DisclosureRegisterRow(
                rs.getLong("id"), rs.getLong("request_id"), rs.getString("reference"), rs.getString("request_type"),
                (Long) rs.getObject("disclosed_by"), rs.getString("recipient"), rs.getString("data_categories"),
                rs.getString("justification"), ts(rs, "disclosed_at")));
        return new PageImpl<>(rows, pageable, count);
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toLocalDateTime();
    }
}
