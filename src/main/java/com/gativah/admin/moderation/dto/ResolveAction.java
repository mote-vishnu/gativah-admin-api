package com.gativah.admin.moderation.dto;

/** The decision an operator can apply when resolving a report. */
public enum ResolveAction {
    DISMISS,     // no violation
    TAKEDOWN,    // soft-delete the content
    WARN,        // record a warning (no removal)
    SUSPEND,     // suspend the author
    BAN,         // ban the author
    REGION_BAN   // geo-restrict the post in a given country (post content only)
}
