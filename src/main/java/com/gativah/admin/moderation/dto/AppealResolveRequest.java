package com.gativah.admin.moderation.dto;

/** Grant lifts the sanction (subject reinstated); deny closes the appeal. */
public record AppealResolveRequest(boolean grant, String note) {
}
