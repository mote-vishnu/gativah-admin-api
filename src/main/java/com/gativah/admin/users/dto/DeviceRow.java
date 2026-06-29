package com.gativah.admin.users.dto;

import java.time.LocalDateTime;

public record DeviceRow(String platform, String appVersion, String locale, LocalDateTime lastSeenAt) {
}
