package com.gativah.admin.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.finance.dto.FinanceRevenueResponse;
import com.gativah.admin.finance.dto.RevenuePoint;
import com.gativah.admin.finance.dto.RevenueSlice;
import com.gativah.admin.finance.query.FinanceQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceServiceImplTest {

    @Mock FinanceQuery query;

    FinanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceServiceImpl(query);
    }

    @Test
    void revenue_without_groupBy_skips_breakdown() {
        when(query.revenueSeries(eq("month"), any(), any())).thenReturn(List.of(new RevenuePoint("2026-06", null, null)));

        FinanceRevenueResponse res = service.revenue(null, null, null, null);

        assertThat(res.granularity()).isEqualTo("month");
        assertThat(res.breakdown()).isEmpty();
        assertThat(res.series()).hasSize(1);
        verify(query, never()).revenueBreakdown(any(), any(), any());
    }

    @Test
    void revenue_with_groupBy_includes_breakdown_and_day_unit() {
        when(query.revenueSeries(eq("day"), any(), any())).thenReturn(List.of());
        when(query.revenueBreakdown(eq("platform"), any(), any()))
                .thenReturn(List.of(new RevenueSlice("iOS", null, 3)));

        FinanceRevenueResponse res = service.revenue("day", null, null, "platform");

        assertThat(res.granularity()).isEqualTo("day");
        assertThat(res.breakdown()).hasSize(1);
    }

    @Test
    void revenue_defaults_to_a_twelve_month_window() {
        when(query.revenueSeries(any(), any(), any())).thenReturn(List.of());

        service.revenue("month", null, null, null);

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(query).revenueSeries(eq("month"), from.capture(), to.capture());
        assertThat(from.getValue()).isBefore(to.getValue());
        assertThat(from.getValue()).isBeforeOrEqualTo(to.getValue().minusMonths(11));
    }

    @Test
    void overview_passes_through() {
        service.overview();
        verify(query).overview();
    }
}
