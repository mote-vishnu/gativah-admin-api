package com.gativah.admin.analytics.service;

import com.gativah.admin.analytics.dto.ActiveUsersResponse;
import com.gativah.admin.analytics.dto.EngagementResponse;
import com.gativah.admin.analytics.dto.EventBreakdownResponse;
import com.gativah.admin.analytics.dto.FunnelResponse;
import com.gativah.admin.analytics.dto.OverviewKpis;
import com.gativah.admin.analytics.dto.PlatformResponse;
import com.gativah.admin.analytics.dto.RetentionResponse;

public interface AnalyticsService {

    OverviewKpis overview(int days);

    ActiveUsersResponse activeUsers(int days);

    EventBreakdownResponse events(int days);

    EngagementResponse engagement(int days);

    RetentionResponse retention(int weeks);

    PlatformResponse platforms(int days);

    FunnelResponse funnel(int days);
}
