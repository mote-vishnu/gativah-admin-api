package com.gativah.admin.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.gativah.admin.analytics.dto.CohortSize;
import com.gativah.admin.analytics.dto.EventBreakdownResponse;
import com.gativah.admin.analytics.dto.EventBreakdownRow;
import com.gativah.admin.analytics.dto.FunnelResponse;
import com.gativah.admin.analytics.dto.OverviewKpis;
import com.gativah.admin.analytics.dto.RetentionCell;
import com.gativah.admin.analytics.dto.RetentionResponse;
import com.gativah.admin.analytics.dto.RetentionRow;
import com.gativah.admin.analytics.query.AnalyticsQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock AnalyticsQuery query;

    AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(query);
    }

    @Test
    void overview_computes_stickiness_deltas_and_avg_events() {
        // 5 distinctActiveUsers calls in order: dau, wau, mau, activeUsers, prevActive
        when(query.distinctActiveUsers(any(), any())).thenReturn(10L, 40L, 100L, 80L, 50L);
        when(query.countSignups(any(), any())).thenReturn(20L, 10L);   // current, previous
        when(query.countEvents(any(), any())).thenReturn(500L, 400L);  // current, previous

        OverviewKpis k = service.overview(30);

        assertThat(k.dau()).isEqualTo(10);
        assertThat(k.wau()).isEqualTo(40);
        assertThat(k.mau()).isEqualTo(100);
        assertThat(k.stickiness()).isEqualTo(10.0);            // 10/100 * 100
        assertThat(k.activeUsers()).isEqualTo(80);
        assertThat(k.activeUsersDelta()).isEqualTo(60.0);      // (80-50)/50
        assertThat(k.newSignups()).isEqualTo(20);
        assertThat(k.newSignupsDelta()).isEqualTo(100.0);      // (20-10)/10
        assertThat(k.totalEvents()).isEqualTo(500);
        assertThat(k.totalEventsDelta()).isEqualTo(25.0);      // (500-400)/400
        assertThat(k.avgEventsPerUser()).isEqualTo(6.3);       // 500/80 = 6.25 → 6.3
    }

    @Test
    void overview_delta_is_plus_100_when_growing_from_zero() {
        when(query.distinctActiveUsers(any(), any())).thenReturn(5L, 5L, 5L, 5L, 0L);
        when(query.countSignups(any(), any())).thenReturn(3L, 0L);
        when(query.countEvents(any(), any())).thenReturn(9L, 0L);

        OverviewKpis k = service.overview(7);

        assertThat(k.activeUsersDelta()).isEqualTo(100.0);
        assertThat(k.newSignupsDelta()).isEqualTo(100.0);
        assertThat(k.totalEventsDelta()).isEqualTo(100.0);
    }

    @Test
    void events_fills_share_percentages_against_the_total() {
        when(query.eventBreakdown(any(), any())).thenReturn(List.of(
                new EventBreakdownRow("app_open", 60, 30, 0),
                new EventBreakdownRow("screen_view", 40, 25, 0)));

        EventBreakdownResponse r = service.events(30);

        assertThat(r.total()).isEqualTo(100);
        assertThat(r.events().get(0).pct()).isEqualTo(60.0);
        assertThat(r.events().get(1).pct()).isEqualTo(40.0);
    }

    @Test
    void funnel_computes_conversion_from_prev_and_start() {
        when(query.funnelUsers(any(), any(), anyList())).thenReturn(Map.of(
                "app_open", 100L, "workout_viewed", 60L, "workout_started", 30L, "workout_completed", 15L));

        FunnelResponse f = service.funnel(30);

        assertThat(f.steps()).hasSize(4);
        assertThat(f.steps().get(0).users()).isEqualTo(100);
        assertThat(f.steps().get(0).conversionFromPrev()).isEqualTo(100.0);
        assertThat(f.steps().get(0).conversionFromStart()).isEqualTo(100.0);
        assertThat(f.steps().get(1).conversionFromPrev()).isEqualTo(60.0);   // 60/100
        assertThat(f.steps().get(2).conversionFromPrev()).isEqualTo(50.0);   // 30/60
        assertThat(f.steps().get(2).conversionFromStart()).isEqualTo(30.0);  // 30/100
        assertThat(f.steps().get(3).conversionFromStart()).isEqualTo(15.0);  // 15/100
    }

    @Test
    void retention_builds_grid_with_percentages_for_observable_cells() {
        LocalDate monday = LocalDate.now().minusDays((long) LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate cohort = monday.minusWeeks(10);   // old enough that all 4 offsets are observable
        when(query.cohortSizes(any())).thenReturn(List.of(new CohortSize(cohort, 10)));
        when(query.retentionCells(any())).thenReturn(List.of(
                new RetentionCell(cohort, 0, 10),
                new RetentionCell(cohort, 1, 7),
                new RetentionCell(cohort, 2, 5),
                new RetentionCell(cohort, 3, 3)));

        RetentionResponse r = service.retention(4);

        assertThat(r.weeks()).isEqualTo(4);
        RetentionRow row = r.cohorts().get(0);
        assertThat(row.cohortSize()).isEqualTo(10);
        assertThat(row.retainedPct()).containsExactly(100.0, 70.0, 50.0, 30.0);
    }

    @Test
    void retention_marks_future_cells_null_for_a_current_week_cohort() {
        LocalDate monday = LocalDate.now().minusDays((long) LocalDate.now().getDayOfWeek().getValue() - 1);
        when(query.cohortSizes(any())).thenReturn(List.of(new CohortSize(monday, 8)));
        when(query.retentionCells(any())).thenReturn(List.of(new RetentionCell(monday, 0, 8)));

        RetentionResponse r = service.retention(4);

        RetentionRow row = r.cohorts().get(0);
        assertThat(row.retained().get(0)).isEqualTo(8L);
        assertThat(row.retained().get(1)).isNull();   // next week not observable yet
        assertThat(row.retainedPct().get(3)).isNull();
    }
}
