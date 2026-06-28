package com.gativah.admin.client;

import java.time.LocalDateTime;

/**
 * Calls pacegrit-service's service-token-guarded {@code /internal/admin/**}
 * hooks so side-effectful changes run the real domain logic there. The acting
 * admin id is forwarded so pacegrit records it on the sanction.
 */
public interface PacegritInternalClient {

    void takedown(Long actorAdminId, String contentType, Long contentId, String reason);

    void suspendUser(Long actorAdminId, Long userId, String reason, LocalDateTime suspendedUntil);

    void banUser(Long actorAdminId, Long userId, String reason);

    void reinstateUser(Long actorAdminId, Long userId);
}
