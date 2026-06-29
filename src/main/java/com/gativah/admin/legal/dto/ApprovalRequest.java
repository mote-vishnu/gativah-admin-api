package com.gativah.admin.legal.dto;

/** Approve or reject a legal request before disclosure. */
public record ApprovalRequest(boolean approve, String note) {
}
