package com.gativah.admin.analytics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gativah.admin.analytics.dto.ActiveUsersResponse;
import com.gativah.admin.analytics.dto.CohortSize;
import com.gativah.admin.analytics.dto.EngagementResponse;
import com.gativah.admin.analytics.dto.EventBreakdownResponse;
import com.gativah.admin.analytics.dto.EventBreakdownRow;
import com.gativah.admin.analytics.dto.FunnelResponse;
import com.gativah.admin.analytics.dto.FunnelStep;
import com.gativah.admin.analytics.dto.OverviewKpis;
import com.gativah.admin.analytics.dto.PlatformResponse;
import com.gativah.admin.analytics.dto.PlatformRow;
import com.gativah.admin.analytics.dto.RetentionCell;
import com.gativah.admin.analytics.dto.RetentionResponse;
import com.gativah.admin.analytics.dto.RetentionRow;
import com.gativah.admin.analytics.query.AnalyticsQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    /** Ordered activation-funnel steps (event name → display label). */
    private static final List<String[]> FUNNEL = List.of(
            new String[] {"app_open", "App open"},
            new String[] {"workout_viewed", "Workout viewed"},
            new String[] {"workout_started", "Workout started"},
            new String[] {"workout_completed", "Workout completed"});

    private final AnalyticsQuery query;

    public AnalyticsServiceImpl(AnalyticsQuery query) {
        this.query = query;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewKpis overview(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(days);
        LocalDateTime prevFrom = now.minusDays(2L * days);

        long dau = query.distinctActiveUsers(now.minusDays(1), now);
        long wau = query.distinctActiveUsers(now.minusDays(7), now);
        long mau = query.distinctActiveUsers(now.minusDays(30), now);
        double stickiness = mau > 0 ? round1(dau * 100.0 / mau) : 0;

        long activeUsers = query.distinctActiveUsers(from, now);
        long prevActive = query.distinctActiveUsers(prevFrom, from);
        long signups = query.countSignups(from, now);
        long prevSignups = query.countSignups(prevFrom, from);
        long events = query.countEvents(from, now);
        long prevEvents = query.countEvents(prevFrom, from);
        double avgEventsPerUser = activeUsers > 0 ? round1((double) events / activeUsers) : 0;

        return new OverviewKpis(dau, wau, mau, stickiness,
                activeUsers, pctDelta(activeUsers, prevActive),
                signups, pctDelta(signups, prevSignups),
                events, pctDelta(events, prevEvents),
                avgEventsPerUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveUsersResponse activeUsers(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);
        return new ActiveUsersResponse(query.activeUsersByDay(from, to), query.signupsByDay(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public EventBreakdownResponse events(int days) {
        LocalDateTime now = LocalDateTime.now();
        List<EventBreakdownRow> raw = query.eventBreakdown(now.minusDays(days), now);
        long total = raw.stream().mapToLong(EventBreakdownRow::count).sum();
        List<EventBreakdownRow> rows = raw.stream()
                .map(r -> new EventBreakdownRow(r.name(), r.count(), r.uniqueUsers(),
                        total > 0 ? round1(r.count() * 100.0 / total) : 0))
                .toList();
        return new EventBreakdownResponse(total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public EngagementResponse engagement(int days) {
        LocalDate to = LocalDate.now();
        return new EngagementResponse(query.stickinessByDay(to.minusDays(days - 1L), to));
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformResponse platforms(int days) {
        LocalDateTime now = LocalDateTime.now();
        List<PlatformRow> raw = query.platformBreakdown(now.minusDays(days), now);
        long total = raw.stream().mapToLong(PlatformRow::events).sum();
        List<PlatformRow> rows = raw.stream()
                .map(r -> new PlatformRow(r.platform(), r.events(), r.users(),
                        total > 0 ? round1(r.events() * 100.0 / total) : 0))
                .toList();
        return new PlatformResponse(rows, query.versionBreakdown(now.minusDays(days), now));
    }

    @Override
    @Transactional(readOnly = true)
    public FunnelResponse funnel(int days) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> users = query.funnelUsers(now.minusDays(days), now,
                FUNNEL.stream().map(s -> s[0]).toList());
        List<FunnelStep> steps = new ArrayList<>();
        long start = users.getOrDefault(FUNNEL.get(0)[0], 0L);
        long prev = 0;
        for (int i = 0; i < FUNNEL.size(); i++) {
            long u = users.getOrDefault(FUNNEL.get(i)[0], 0L);
            double fromPrev = i == 0 ? 100 : (prev > 0 ? round1(u * 100.0 / prev) : 0);
            double fromStart = start > 0 ? round1(u * 100.0 / start) : 0;
            steps.add(new FunnelStep(FUNNEL.get(i)[0], FUNNEL.get(i)[1], u, fromPrev, fromStart));
            prev = u;
        }
        return new FunnelResponse(steps);
    }

    @Override
    @Transactional(readOnly = true)
    public RetentionResponse retention(int weeks) {
        LocalDateTime from = LocalDateTime.now().minusWeeks(weeks);
        List<CohortSize> sizes = query.cohortSizes(from);
        List<RetentionCell> cells = query.retentionCells(from);
        LocalDate currentWeek = LocalDate.now().minusDays((long) LocalDate.now().getDayOfWeek().getValue() - 1);

        List<RetentionRow> rows = new ArrayList<>();
        for (CohortSize c : sizes) {
            long size = c.size();
            long maxOffset = ChronoUnit.WEEKS.between(c.cohortWeek(), currentWeek);
            List<Long> retained = new ArrayList<>();
            List<Double> pct = new ArrayList<>();
            for (int o = 0; o < weeks; o++) {
                if (o > maxOffset) {
                    retained.add(null);   // future cell — not yet observable
                    pct.add(null);
                    continue;
                }
                long r = cellValue(cells, c.cohortWeek(), o);
                retained.add(r);
                pct.add(size > 0 ? round1(r * 100.0 / size) : 0);
            }
            rows.add(new RetentionRow(c.cohortWeek(), size, retained, pct));
        }
        return new RetentionResponse(weeks, rows);
    }

    private static long cellValue(List<RetentionCell> cells, LocalDate cohortWeek, int offset) {
        return cells.stream()
                .filter(x -> x.cohortWeek().equals(cohortWeek) && x.weekOffset() == offset)
                .mapToLong(RetentionCell::retained)
                .findFirst()
                .orElse(0);
    }

    /** Percentage change of {@code cur} vs {@code prev}; +100 when growing from zero. */
    private static double pctDelta(long cur, long prev) {
        if (prev == 0) {
            return cur > 0 ? 100 : 0;
        }
        return round1((cur - prev) * 100.0 / prev);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
