package com.gativah.admin.analytics.service;

import java.util.Locale;

/** ISO-3166 alpha-2 → display name, backed by the JDK locale database. */
final class Countries {

    private Countries() {
    }

    /** English display name for an ISO alpha-2 code, or the code itself if unknown. */
    static String name(String code) {
        if (code == null || code.isBlank()) {
            return "Unknown";
        }
        String display = new Locale("", code.toUpperCase()).getDisplayCountry(Locale.ENGLISH);
        return display.isBlank() || display.equalsIgnoreCase(code) ? code.toUpperCase() : display;
    }
}
