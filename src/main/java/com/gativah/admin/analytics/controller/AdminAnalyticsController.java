package com.gativah.admin.analytics.controller;

import com.gativah.admin.analytics.dto.ActiveUsersResponse;
import com.gativah.admin.analytics.dto.EngagementResponse;
import com.gativah.admin.analytics.dto.EventBreakdownResponse;
import com.gativah.admin.analytics.dto.FunnelResponse;
import com.gativah.admin.analytics.dto.GeoResponse;
import com.gativah.admin.analytics.dto.OverviewKpis;
import com.gativah.admin.analytics.dto.PlatformResponse;
import com.gativah.admin.analytics.dto.RetentionResponse;
import com.gativah.admin.analytics.service.AnalyticsService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Product-analytics dashboards over the analytics_event sink. All read-only (ANALYTICS:VIEW). */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasAuthority('ANALYTICS:VIEW')")
public class AdminAnalyticsController {

    private final AnalyticsService service;

    public AdminAnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public OverviewKpis overview(@RequestParam(defaultValue = "30") int days) {
        return service.overview(clampDays(days));
    }

    @GetMapping("/active-users")
    public ActiveUsersResponse activeUsers(@RequestParam(defaultValue = "30") int days) {
        return service.activeUsers(clampDays(days));
    }

    @GetMapping("/events")
    public EventBreakdownResponse events(@RequestParam(defaultValue = "30") int days) {
        return service.events(clampDays(days));
    }

    @GetMapping("/engagement")
    public EngagementResponse engagement(@RequestParam(defaultValue = "30") int days) {
        return service.engagement(clampDays(days));
    }

    @GetMapping("/retention")
    public RetentionResponse retention(@RequestParam(defaultValue = "8") int weeks) {
        return service.retention(Math.min(Math.max(weeks, 2), 26));
    }

    @GetMapping("/platforms")
    public PlatformResponse platforms(@RequestParam(defaultValue = "30") int days) {
        return service.platforms(clampDays(days));
    }

    @GetMapping("/funnel")
    public FunnelResponse funnel(@RequestParam(defaultValue = "30") int days) {
        return service.funnel(clampDays(days));
    }

    @GetMapping("/geo")
    public GeoResponse geo() {
        return service.geo();
    }

    private static int clampDays(int days) {
        return Math.min(Math.max(days, 1), 365);
    }
}
