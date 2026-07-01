package com.gativah.admin.client;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** RestClient-backed implementation; injects the shared service token per call. */
@Component
public class PacegritInternalClientImpl implements PacegritInternalClient {

    private final RestClient client;
    private final String serviceToken;

    public PacegritInternalClientImpl(
            @Value("${pacegrit.internal.base-url:http://localhost:8081}") String baseUrl,
            @Value("${pacegrit.internal.service-token:}") String serviceToken) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
    }

    private record TakedownBody(String type, Long id, String reason) {
    }

    private record RegionBanBody(Long postId, String country, String reason) {
    }

    private record SuspendBody(String reason, LocalDateTime suspendedUntil) {
    }

    private record BanBody(String reason) {
    }

    private record CompBody(Long userId, String code, LocalDateTime expiresAt, String reason) {
    }

    private record RevokeBody(Long userId, String code) {
    }

    private record VerifiedBody(boolean grant) {
    }

    private record ClubReasonBody(String reason) {
    }


    @Override
    public void takedown(Long actorAdminId, String contentType, Long contentId, String reason) {
        post("/internal/admin/content/takedown", actorAdminId, new TakedownBody(contentType, contentId, reason));
    }

    @Override
    public void regionBan(Long actorAdminId, Long postId, String country, String reason) {
        post("/internal/admin/content/region-ban", actorAdminId, new RegionBanBody(postId, country, reason));
    }

    @Override
    public void liftRegionBan(Long actorAdminId, Long regionBanId) {
        post("/internal/admin/region-bans/" + regionBanId + "/lift", actorAdminId, null);
    }

    @Override
    public void suspendUser(Long actorAdminId, Long userId, String reason, LocalDateTime suspendedUntil) {
        post("/internal/admin/users/" + userId + "/suspend", actorAdminId, new SuspendBody(reason, suspendedUntil));
    }

    @Override
    public void banUser(Long actorAdminId, Long userId, String reason) {
        post("/internal/admin/users/" + userId + "/ban", actorAdminId, new BanBody(reason));
    }

    @Override
    public void reinstateUser(Long actorAdminId, Long userId) {
        post("/internal/admin/users/" + userId + "/reinstate", actorAdminId, null);
    }

    @Override
    public void grantComp(Long actorAdminId, Long userId, String code, LocalDateTime expiresAt, String reason) {
        post("/internal/admin/entitlements/comp", actorAdminId, new CompBody(userId, code, expiresAt, reason));
    }

    @Override
    public void revokeComp(Long actorAdminId, Long userId, String code) {
        post("/internal/admin/entitlements/revoke", actorAdminId, new RevokeBody(userId, code));
    }

    @Override
    public void setVerified(Long actorAdminId, Long userId, boolean grant) {
        post("/internal/admin/users/" + userId + "/verified", actorAdminId, new VerifiedBody(grant));
    }

    @Override
    public void removeClub(Long actorAdminId, Long clubId, String reason) {
        post("/internal/admin/clubs/" + clubId + "/remove", actorAdminId, new ClubReasonBody(reason));
    }

    @Override
    public void restoreClub(Long actorAdminId, Long clubId) {
        post("/internal/admin/clubs/" + clubId + "/restore", actorAdminId, null);
    }

    @Override
    public void removeClubMember(Long actorAdminId, Long clubId, Long userId) {
        post("/internal/admin/clubs/" + clubId + "/members/" + userId + "/remove", actorAdminId, null);
    }


    @Override
    public void removeClubEvent(Long actorAdminId, Long clubId, Long eventId, String reason) {
        post("/internal/admin/clubs/" + clubId + "/events/" + eventId + "/remove", actorAdminId, new ClubReasonBody(reason));
    }

    @Override
    public void restoreClubEvent(Long actorAdminId, Long clubId, Long eventId) {
        post("/internal/admin/clubs/" + clubId + "/events/" + eventId + "/restore", actorAdminId, null);
    }

    private void post(String path, Long actorAdminId, Object body) {
        RestClient.RequestBodySpec spec = client.post()
                .uri(path)
                .header("X-Service-Token", serviceToken)
                .header("X-Admin-Actor-Id", String.valueOf(actorAdminId))
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            spec.body(body);
        }
        spec.retrieve().toBodilessEntity();
    }
}
