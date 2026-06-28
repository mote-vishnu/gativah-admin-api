package com.gativah.admin.auth.model;

/** Fine-grained capabilities granted to staff roles (carried as JWT authorities). */
public enum AdminPermission {
    MODERATION_REVIEW,   // view the report queue / case detail
    MODERATION_ACTION,   // dismiss / take down / warn / suspend / ban
    APPEAL_HANDLE,       // resolve appeals
    FINANCE_VIEW,        // revenue / subscriptions / transactions
    ENTITLEMENT_GRANT,   // comp / promo entitlements
    STAFF_MANAGE,        // manage admin accounts & roles
    AUDIT_VIEW_ALL,      // view the full operator audit log
    LEGAL_ACCESS         // legal & lawful-disclosure (most restricted)
}
