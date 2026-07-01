package com.gativah.admin.client;

import java.time.LocalDateTime;

/**
 * Calls pacegrit-service's service-token-guarded {@code /internal/admin/**}
 * hooks so side-effectful changes run the real domain logic there. The acting
 * admin id is forwarded so pacegrit records it on the sanction.
 */
public interface PacegritInternalClient {

    void takedown(Long actorAdminId, String contentType, Long contentId, String reason);

    void regionBan(Long actorAdminId, Long postId, String country, String reason);

    void liftRegionBan(Long actorAdminId, Long regionBanId);

    void suspendUser(Long actorAdminId, Long userId, String reason, LocalDateTime suspendedUntil);

    void banUser(Long actorAdminId, Long userId, String reason);

    void reinstateUser(Long actorAdminId, Long userId);

    void grantComp(Long actorAdminId, Long userId, String code, LocalDateTime expiresAt, String reason);

    void revokeComp(Long actorAdminId, Long userId, String code);

    void setVerified(Long actorAdminId, Long userId, boolean grant);

    void removeClub(Long actorAdminId, Long clubId, String reason);

    void restoreClub(Long actorAdminId, Long clubId);

    void removeClubMember(Long actorAdminId, Long clubId, Long userId);

    void removeClubEvent(Long actorAdminId, Long clubId, Long eventId, String reason);

    void restoreClubEvent(Long actorAdminId, Long clubId, Long eventId);
}
