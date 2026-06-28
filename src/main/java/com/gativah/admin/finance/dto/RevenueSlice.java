package com.gativah.admin.finance.dto;

import java.math.BigDecimal;

/** One slice of a revenue breakdown (by product / platform / country / currency). */
public record RevenueSlice(String key, BigDecimal gross, long count) {
}
