package com.gativah.admin.security.dto;

import java.util.List;

/** Org-wide admin security posture. */
public record SecurityOverview(
        long mfaEnrolled,
        long mfaTotal,
        long activeAdmins,
        long activeSessions,
        long signIns7d,
        List<UnenrolledAdmin> unenrolled) {
}
