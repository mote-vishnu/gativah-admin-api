package com.gativah.admin.users.dto;

import java.time.LocalDate;

public record ActivityPoint(LocalDate date, long steps, int activeMinutes) {
}
