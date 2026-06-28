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

    private record SuspendBody(String reason, LocalDateTime suspendedUntil) {
    }

    private record BanBody(String reason) {
    }

    @Override
    public void takedown(Long actorAdminId, String contentType, Long contentId, String reason) {
        post("/internal/admin/content/takedown", actorAdminId, new TakedownBody(contentType, contentId, reason));
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
