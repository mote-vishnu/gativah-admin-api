package com.gativah.admin.billing.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.billing.dto.EntitlementDefRow;
import com.gativah.admin.billing.dto.EntitlementRow;
import com.gativah.admin.billing.dto.RefundRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native-SQL read side for Billing Ops (entitlement registry + refunds). */
@Repository
public class BillingQueryJdbc implements BillingQuery {

    private static final String ENT_FROM = """
            from user_entitlement ue
            left join entitlement_def ed on ed.code = ue.entitlement_code
            left join user_account u on u.id = ue.user_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public BillingQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<EntitlementRow> entitlements(String q, String source, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("q", q);
        String filter = "where (cast(:q as varchar) is null or u.username ilike :q or ue.entitlement_code ilike :q)";
        if ("COMP".equalsIgnoreCase(source)) {
            filter += " and ue.source_subscription_id is null";
        } else if ("SUBSCRIPTION".equalsIgnoreCase(source)) {
            filter += " and ue.source_subscription_id is not null";
        }

        Long total = jdbc.queryForObject("select count(*) " + ENT_FROM + filter, params, Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select ue.id, ue.user_id, u.username, ue.entitlement_code, ed.name, ue.active, "
                + "case when ue.source_subscription_id is null then 'COMP' else 'SUBSCRIPTION' end as source, "
                + "ue.expires_at, ue.updated_at "
                + ENT_FROM + filter + " order by ue.updated_at desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<EntitlementRow> rows = jdbc.query(sql, params, (rs, i) -> new EntitlementRow(
                rs.getLong("id"),
                (Long) rs.getObject("user_id"),
                rs.getString("username"),
                rs.getString("entitlement_code"),
                rs.getString("name"),
                rs.getBoolean("active"),
                rs.getString("source"),
                ts(rs, "expires_at"),
                ts(rs, "updated_at")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public Page<RefundRow> refunds(Pageable pageable) {
        Long total = jdbc.queryForObject(
                "select count(*) from billing_transaction where type in ('REFUND','CHARGEBACK')",
                new MapSqlParameterSource(), Long.class);
        long count = total == null ? 0 : total;
        if (count == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String sql = "select bt.id, bt.user_id, u.username, bt.plan_code, bt.type, bt.gross_amount, "
                + "bt.gross_currency, bt.country_code, bt.purchased_at "
                + "from billing_transaction bt left join user_account u on u.id = bt.user_id "
                + "where bt.type in ('REFUND','CHARGEBACK') order by bt.purchased_at desc limit :limit offset :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        List<RefundRow> rows = jdbc.query(sql, params, (rs, i) -> new RefundRow(
                rs.getLong("id"),
                (Long) rs.getObject("user_id"),
                rs.getString("username"),
                rs.getString("plan_code"),
                rs.getString("type"),
                rs.getBigDecimal("gross_amount"),
                rs.getString("gross_currency"),
                rs.getString("country_code"),
                ts(rs, "purchased_at")));
        return new PageImpl<>(rows, pageable, count);
    }

    @Override
    public List<EntitlementDefRow> entitlementDefs() {
        return jdbc.query("select code, name from entitlement_def where active = true order by name",
                new MapSqlParameterSource(),
                (rs, i) -> new EntitlementDefRow(rs.getString("code"), rs.getString("name")));
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toLocalDateTime();
    }
}
