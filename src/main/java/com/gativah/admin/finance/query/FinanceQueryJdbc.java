package com.gativah.admin.finance.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.gativah.admin.finance.dto.DeadLetterRow;
import com.gativah.admin.finance.dto.FinanceOverview;
import com.gativah.admin.finance.dto.MrrMovement;
import com.gativah.admin.finance.dto.PayoutRow;
import com.gativah.admin.finance.dto.PayoutsResponse;
import com.gativah.admin.finance.dto.RevenuePoint;
import com.gativah.admin.finance.dto.RevenueSlice;
import com.gativah.admin.finance.dto.SubscriptionRow;
import com.gativah.admin.finance.dto.TransactionDetail;
import com.gativah.admin.finance.dto.TransactionRow;
import com.gativah.admin.finance.dto.TxnEventRow;
import com.gativah.admin.finance.dto.WebhookHealth;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL aggregation over billing_transaction / subscription / billing_event.
 * All amounts are store-gross. The "positive" revenue types are purchase-like;
 * refunds + chargebacks are reported separately and netted in the overview.
 */
@Repository
public class FinanceQueryJdbc implements FinanceQuery {

    // Subscription states that currently grant access (mirror pacegrit's ACCESS_STATES).
    private static final String ACCESS_STATES = "('ACTIVE','TRIALING','CANCELED')";
    private static final String POSITIVE_TYPES = "('PURCHASE','RENEWAL','RESUBSCRIBE','UPGRADE')";
    private static final String REFUND_TYPES = "('REFUND','CHARGEBACK')";

    private final NamedParameterJdbcTemplate jdbc;

    public FinanceQueryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public FinanceOverview overview() {
        long active = count("select count(*) from subscription where state in " + ACCESS_STATES);
        long trialing = count("select count(*) from subscription where state = 'TRIALING'");
        long inGrace = count("select count(*) from subscription where state = 'IN_GRACE_PERIOD'");
        long canceled30d = count("select count(*) from subscription where state in ('CANCELED','EXPIRED','REVOKED') "
                + "and updated_at >= now() - interval '30 days'");
        long newSubs30d = count("select count(*) from subscription where created_at >= now() - interval '30 days'");
        // Monthly churn proxy: of those active at the period start (active now + churned), what fraction churned.
        double churnRate = (active + canceled30d) == 0 ? 0.0
                : Math.round(canceled30d * 10000.0 / (active + canceled30d)) / 100.0;

        // Gross trend: last-30d vs the prior 30d (store-gross, positive types).
        BigDecimal gross30 = money("select coalesce(sum(gross_amount),0) from billing_transaction "
                + "where type in " + POSITIVE_TYPES + " and status = 'VALID' and purchased_at >= now() - interval '30 days'");
        BigDecimal grossPrev30 = money("select coalesce(sum(gross_amount),0) from billing_transaction "
                + "where type in " + POSITIVE_TYPES + " and status = 'VALID' "
                + "and purchased_at >= now() - interval '60 days' and purchased_at < now() - interval '30 days'");
        Double grossTrendPct = grossPrev30.signum() == 0 ? null
                : gross30.subtract(grossPrev30).multiply(BigDecimal.valueOf(100))
                        .divide(grossPrev30, 1, RoundingMode.HALF_UP).doubleValue();

        BigDecimal grossMtd = money("select coalesce(sum(gross_amount),0) from billing_transaction "
                + "where type in " + POSITIVE_TYPES + " and status = 'VALID' "
                + "and purchased_at >= date_trunc('month', now())");
        BigDecimal refundsMtd = money("select coalesce(sum(gross_amount),0) from billing_transaction "
                + "where type in " + REFUND_TYPES + " and purchased_at >= date_trunc('month', now())");

        // MRR: normalise each active plan's display price to a monthly figure.
        BigDecimal mrr = money("""
                select coalesce(sum(sp.price_amount / case sp.period
                            when 'ANNUAL' then 12 when 'SEMIANNUAL' then 6
                            when 'QUARTERLY' then 3 when 'MONTHLY' then 1 end), 0)
                from subscription s
                join subscription_plan sp on sp.code = s.plan_code
                where s.state in """ + ACCESS_STATES + """
                  and sp.period <> 'LIFETIME' and sp.price_amount is not null
                """);

        return new FinanceOverview(active, trialing, inGrace, canceled30d, newSubs30d, churnRate, grossTrendPct,
                mrr, mrr.multiply(BigDecimal.valueOf(12)), grossMtd, refundsMtd, grossMtd.subtract(refundsMtd));
    }

    @Override
    public MrrMovement mrrMovement() {
        BigDecimal end = monthlyMrr("s.state in " + ACCESS_STATES);
        BigDecimal added = monthlyMrr("s.state in " + ACCESS_STATES + " and s.created_at >= now() - interval '30 days'");
        // Truly lost access in the window (CANCELED still has paid access in this model).
        BigDecimal churned = monthlyMrr("s.state in ('EXPIRED','REVOKED') and s.updated_at >= now() - interval '30 days'");
        BigDecimal start = end.subtract(added).add(churned);
        return new MrrMovement(start, added, churned, end);
    }

    @Override
    public PayoutsResponse payouts(int windowDays) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("days", windowDays);
        String sql = "select coalesce(platform,'UNKNOWN') as platform, "
                + "coalesce(sum(gross_amount) filter (where type in " + POSITIVE_TYPES + " and status='VALID'),0) as gross, "
                + "coalesce(sum(gross_amount) filter (where type in " + REFUND_TYPES + "),0) as refunds, "
                + "count(*) filter (where type in " + POSITIVE_TYPES + " and status='VALID') as cnt "
                + "from billing_transaction "
                + "where purchased_at >= now() - make_interval(days => :days) "
                + "group by coalesce(platform,'UNKNOWN') order by gross desc";

        List<PayoutRow> rows = jdbc.query(sql, p, (rs, i) -> {
            String platform = rs.getString("platform");
            BigDecimal gross = rs.getBigDecimal("gross");
            BigDecimal refunds = rs.getBigDecimal("refunds");
            BigDecimal netGross = gross.subtract(refunds);
            double rate = commissionRate(platform);
            BigDecimal commission = netGross.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal payout = netGross.subtract(commission);
            return new PayoutRow(platform, gross, refunds, netGross, rs.getLong("cnt"), rate, commission, payout);
        });

        BigDecimal grossTotal = BigDecimal.ZERO, refundTotal = BigDecimal.ZERO,
                netTotal = BigDecimal.ZERO, commissionTotal = BigDecimal.ZERO, payoutTotal = BigDecimal.ZERO;
        for (PayoutRow r : rows) {
            grossTotal = grossTotal.add(r.gross());
            refundTotal = refundTotal.add(r.refunds());
            netTotal = netTotal.add(r.netGross());
            commissionTotal = commissionTotal.add(r.commission());
            payoutTotal = payoutTotal.add(r.payout());
        }
        return new PayoutsResponse(windowDays, rows, grossTotal, refundTotal, netTotal, commissionTotal, payoutTotal);
    }

    /** Estimated store commission rate by platform (standard first-year/non-program rates). */
    private static double commissionRate(String platform) {
        if (platform == null) {
            return 0.0;
        }
        return switch (platform.toUpperCase()) {
            case "APPLE", "APP_STORE", "IOS", "GOOGLE", "GOOGLE_PLAY", "PLAY", "ANDROID" -> 0.30;
            case "STRIPE", "WEB", "PADDLE" -> 0.029;
            default -> 0.0;
        };
    }

    /** Sum of normalized monthly plan price for subscriptions matching {@code whereExtra}. */
    private BigDecimal monthlyMrr(String whereExtra) {
        return money("""
                select coalesce(sum(sp.price_amount / case sp.period
                            when 'ANNUAL' then 12 when 'SEMIANNUAL' then 6
                            when 'QUARTERLY' then 3 when 'MONTHLY' then 1 end), 0)
                from subscription s
                join subscription_plan sp on sp.code = s.plan_code
                where """ + whereExtra + " and sp.period <> 'LIFETIME' and sp.price_amount is not null");
    }

    @Override
    public List<RevenuePoint> revenueSeries(String granularity, LocalDateTime from, LocalDateTime to) {
        String unit = "day".equalsIgnoreCase(granularity) ? "day" : "month";
        DateTimeFormatter fmt = unit.equals("day")
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd") : DateTimeFormatter.ofPattern("yyyy-MM");
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("unit", unit).addValue("from", from).addValue("to", to);
        String sql = "select date_trunc(cast(:unit as text), purchased_at) as bucket, "
                + "coalesce(sum(gross_amount) filter (where type in " + POSITIVE_TYPES + " and status='VALID'),0) as gross, "
                + "coalesce(sum(gross_amount) filter (where type in " + REFUND_TYPES + "),0) as refunds "
                + "from billing_transaction where purchased_at >= :from and purchased_at < :to "
                + "group by bucket order by bucket";
        return jdbc.query(sql, p, (rs, i) -> {
            LocalDateTime bucket = rs.getTimestamp("bucket").toLocalDateTime();
            return new RevenuePoint(bucket.format(fmt), rs.getBigDecimal("gross"), rs.getBigDecimal("refunds"));
        });
    }

    @Override
    public List<RevenueSlice> revenueBreakdown(String groupBy, LocalDateTime from, LocalDateTime to) {
        String col = switch (groupBy == null ? "" : groupBy.toLowerCase()) {
            case "product" -> "plan_code";
            case "platform" -> "platform";
            case "country" -> "country_code";
            case "currency" -> "gross_currency";
            default -> throw new IllegalArgumentException("Unsupported groupBy: " + groupBy);
        };
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("from", from).addValue("to", to);
        String sql = "select " + col + " as key, "
                + "coalesce(sum(gross_amount) filter (where type in " + POSITIVE_TYPES + " and status='VALID'),0) as gross, "
                + "count(*) as cnt from billing_transaction "
                + "where purchased_at >= :from and purchased_at < :to group by " + col + " order by gross desc nulls last";
        return jdbc.query(sql, p, (rs, i) ->
                new RevenueSlice(rs.getString("key"), rs.getBigDecimal("gross"), rs.getLong("cnt")));
    }

    // Whitelisted ORDER BY for the client-supplied sort key (never raw-interpolated).
    private static String txnOrder(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            var o = pageable.getSort().iterator().next();
            String col = switch (o.getProperty()) {
                case "gross" -> "gross_amount";
                case "type" -> "type";
                case "platform" -> "platform";
                default -> "purchased_at";
            };
            return col + (o.isAscending() ? " asc" : " desc");
        }
        return "purchased_at desc";
    }

    private static String subOrder(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            var o = pageable.getSort().iterator().next();
            String col = switch (o.getProperty()) {
                case "renews" -> "current_period_end";
                case "state" -> "state";
                case "platform" -> "platform";
                default -> "updated_at";
            };
            return col + (o.isAscending() ? " asc" : " desc");
        }
        return "updated_at desc";
    }

    @Override
    public Page<TransactionRow> transactions(String platform, String type, String status, String country,
                                             Long userId, Pageable pageable) {
        String where = "where (cast(:platform as varchar) is null or platform=:platform) "
                + "and (cast(:type as varchar) is null or type=:type) and (cast(:status as varchar) is null or status=:status) "
                + "and (cast(:country as varchar) is null or country_code=:country) "
                + "and (cast(:userId as bigint) is null or user_id=:userId) ";
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("platform", platform).addValue("type", type).addValue("status", status)
                .addValue("country", country).addValue("userId", userId);
        long total = count("select count(*) from billing_transaction " + where, p);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String sql = "select id,user_id,plan_code,platform,type,status,gross_amount,gross_currency,country_code,"
                + "source,purchased_at from billing_transaction " + where
                + "order by " + txnOrder(pageable) + " limit :limit offset :offset";
        List<TransactionRow> rows = jdbc.query(sql, p, (rs, i) -> new TransactionRow(
                rs.getLong("id"), (Long) rs.getObject("user_id"), rs.getString("plan_code"),
                rs.getString("platform"), rs.getString("type"), rs.getString("status"),
                rs.getBigDecimal("gross_amount"), rs.getString("gross_currency"), rs.getString("country_code"),
                rs.getString("source"), ts(rs.getTimestamp("purchased_at"))));
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public TransactionDetail transactionDetail(long id) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("id", id);
        String sql = "select id,user_id,subscription_id,plan_code,platform,product_id,store_transaction_id,"
                + "original_transaction_id,type,status,gross_amount,gross_currency,country_code,period_start,"
                + "period_end,purchased_at,environment,source,notification_uuid,created_at "
                + "from billing_transaction where id = :id";
        List<TransactionDetail> found = jdbc.query(sql, p, (rs, i) -> mapDetail(rs));
        if (found.isEmpty()) {
            return null;
        }
        TransactionDetail base = found.get(0);

        SubscriptionRow subscription = base.subscriptionId() == null ? null : subscriptionById(base.subscriptionId());
        List<TransactionRow> related = base.originalTransactionId() == null ? List.of()
                : relatedTransactions(base.originalTransactionId(), id);
        List<TxnEventRow> events = matchedEvents(base.originalTransactionId(), base.notificationUuid());

        return new TransactionDetail(base.id(), base.userId(), base.subscriptionId(), base.planCode(),
                base.platform(), base.productId(), base.storeTransactionId(), base.originalTransactionId(),
                base.type(), base.status(), base.grossAmount(), base.grossCurrency(), base.countryCode(),
                base.periodStart(), base.periodEnd(), base.purchasedAt(), base.environment(), base.source(),
                base.notificationUuid(), base.createdAt(), subscription, related, events);
    }

    private TransactionDetail mapDetail(ResultSet rs) throws SQLException {
        return new TransactionDetail(rs.getLong("id"), (Long) rs.getObject("user_id"),
                (Long) rs.getObject("subscription_id"), rs.getString("plan_code"), rs.getString("platform"),
                rs.getString("product_id"), rs.getString("store_transaction_id"),
                rs.getString("original_transaction_id"), rs.getString("type"), rs.getString("status"),
                rs.getBigDecimal("gross_amount"), rs.getString("gross_currency"), rs.getString("country_code"),
                ts(rs.getTimestamp("period_start")), ts(rs.getTimestamp("period_end")),
                ts(rs.getTimestamp("purchased_at")), rs.getString("environment"), rs.getString("source"),
                rs.getString("notification_uuid"), ts(rs.getTimestamp("created_at")), null, List.of(), List.of());
    }

    private SubscriptionRow subscriptionById(long subscriptionId) {
        List<SubscriptionRow> rows = jdbc.query(
                "select id,user_id,plan_code,platform,state,auto_renew,is_trial,current_period_end "
                        + "from subscription where id = :id",
                new MapSqlParameterSource().addValue("id", subscriptionId),
                (rs, i) -> new SubscriptionRow(rs.getLong("id"), (Long) rs.getObject("user_id"),
                        rs.getString("plan_code"), rs.getString("platform"), rs.getString("state"),
                        rs.getBoolean("auto_renew"), rs.getBoolean("is_trial"),
                        ts(rs.getTimestamp("current_period_end"))));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<TransactionRow> relatedTransactions(String originalTxnId, long excludeId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("otid", originalTxnId).addValue("id", excludeId);
        return jdbc.query("select id,user_id,plan_code,platform,type,status,gross_amount,gross_currency,"
                + "country_code,source,purchased_at from billing_transaction "
                + "where original_transaction_id = :otid and id <> :id order by purchased_at desc limit 20", p,
                (rs, i) -> new TransactionRow(rs.getLong("id"), (Long) rs.getObject("user_id"),
                        rs.getString("plan_code"), rs.getString("platform"), rs.getString("type"),
                        rs.getString("status"), rs.getBigDecimal("gross_amount"), rs.getString("gross_currency"),
                        rs.getString("country_code"), rs.getString("source"), ts(rs.getTimestamp("purchased_at"))));
    }

    private List<TxnEventRow> matchedEvents(String originalTxnId, String notificationUuid) {
        if (originalTxnId == null && notificationUuid == null) {
            return List.of();
        }
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("otid", originalTxnId).addValue("nuid", notificationUuid);
        return jdbc.query("select id,platform,event_type,subtype,status,received_at,processed_at "
                + "from billing_event where (cast(:otid as varchar) is not null and store_subscription_id = :otid) "
                + "or (cast(:nuid as varchar) is not null and store_notification_id = :nuid) "
                + "order by received_at desc limit 20", p,
                (rs, i) -> new TxnEventRow(rs.getLong("id"), rs.getString("platform"), rs.getString("event_type"),
                        rs.getString("subtype"), rs.getString("status"),
                        ts(rs.getTimestamp("received_at")), ts(rs.getTimestamp("processed_at"))));
    }

    @Override
    public Page<SubscriptionRow> subscriptions(String state, Pageable pageable) {
        String where = "where (cast(:state as varchar) is null or state=:state) ";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("state", state);
        long total = count("select count(*) from subscription " + where, p);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String sql = "select id,user_id,plan_code,platform,state,auto_renew,is_trial,current_period_end "
                + "from subscription " + where + "order by " + subOrder(pageable) + " limit :limit offset :offset";
        List<SubscriptionRow> rows = jdbc.query(sql, p, (rs, i) -> new SubscriptionRow(
                rs.getLong("id"), (Long) rs.getObject("user_id"), rs.getString("plan_code"),
                rs.getString("platform"), rs.getString("state"), rs.getBoolean("auto_renew"),
                rs.getBoolean("is_trial"), ts(rs.getTimestamp("current_period_end"))));
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public WebhookHealth webhookHealth() {
        long received = count("select count(*) from billing_event where received_at >= now() - interval '24 hours'");
        long processed = count("select count(*) from billing_event where status='PROCESSED' "
                + "and processed_at >= now() - interval '24 hours'");
        long failed = count("select count(*) from billing_event where status='FAILED' "
                + "and received_at >= now() - interval '24 hours'");
        long dead = count("select count(*) from billing_event where status='DEAD_LETTER'");
        LocalDateTime lastProcessed = ts(jdbc.queryForObject(
                "select max(processed_at) from billing_event", new MapSqlParameterSource(), Timestamp.class));
        List<DeadLetterRow> recent = jdbc.query(
                "select id,platform,event_type,attempts,last_error,received_at from billing_event "
                        + "where status='DEAD_LETTER' order by received_at desc limit 20",
                new MapSqlParameterSource(), (rs, i) -> new DeadLetterRow(
                        rs.getLong("id"), rs.getString("platform"), rs.getString("event_type"),
                        rs.getInt("attempts"), rs.getString("last_error"), ts(rs.getTimestamp("received_at"))));
        return new WebhookHealth(received, processed, failed, dead, lastProcessed, recent);
    }

    // ── helpers ───────────────────────────────────────────────
    private long count(String sql) {
        return count(sql, new MapSqlParameterSource());
    }

    private long count(String sql, MapSqlParameterSource p) {
        Long n = jdbc.queryForObject(sql, p, Long.class);
        return n == null ? 0 : n;
    }

    private BigDecimal money(String sql) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, new MapSqlParameterSource(), BigDecimal.class);
            return v == null ? BigDecimal.ZERO : v;
        } catch (EmptyResultDataAccessException e) {
            return BigDecimal.ZERO;
        }
    }

    private static LocalDateTime ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
