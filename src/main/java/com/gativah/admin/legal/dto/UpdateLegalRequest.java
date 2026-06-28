package com.gativah.admin.legal.dto;

/** Partial update — null fields are left unchanged. status is one of the LegalRequest.STATUS_* values. */
public record UpdateLegalRequest(String status, String notes) {
}
