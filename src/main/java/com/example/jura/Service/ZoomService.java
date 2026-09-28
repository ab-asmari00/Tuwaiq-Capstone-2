package com.example.jura.Service;

import com.example.jura.Model.Appointment;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ZoomService {

    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");
    private static final String PASSCODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final RestClient restClient;
    private final SecureRandom random = new SecureRandom();
    private final String accountId;
    private final String clientId;
    private final String clientSecret;
    private final String hostUserId;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public ZoomService(RestClient.Builder restClientBuilder,
                       @Value("${zoom.account-id:}") String accountId,
                       @Value("${zoom.client-id:}") String clientId,
                       @Value("${zoom.client-secret:}") String clientSecret,
                       @Value("${zoom.host-user-id:}") String hostUserId) {
        this.restClient = restClientBuilder.build();
        this.accountId = accountId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.hostUserId = hostUserId;
    }

    public boolean isConfigured() {
        return !accountId.isBlank() && !clientId.isBlank()
                && !clientSecret.isBlank() && !hostUserId.isBlank();
    }

    public ZoomMeetingDetails createMeeting(Appointment appointment) {
        if (!isConfigured()) {
            throw new IllegalStateException("Zoom credentials or host user ID are missing");
        }

        long seconds = Duration.between(appointment.getStartAt(), appointment.getEndAt()).getSeconds();
        long durationMinutes = (seconds + 59) / 60;
        if (durationMinutes < 1 || durationMinutes > 1440) {
            throw new IllegalArgumentException("Zoom meeting duration must be between 1 and 1440 minutes");
        }

        String startTime = DateTimeFormatter.ISO_INSTANT.format(
                appointment.getStartAt().atZone(RIYADH).toInstant());

        Map<String, Object> request = Map.of(
                "topic", "Jur'a consultation",
                "type", 2,
                "start_time", startTime,
                "duration", durationMinutes,
                "password", newPasscode(),
                "settings", Map.of("join_before_host", true, "jbh_time", 5, "waiting_room", false)
        );

        Map<?, ?> response = restClient.post()
                .uri("https://api.zoom.us/v2/users/{userId}/meetings", hostUserId)
                .headers(headers -> headers.setBearerAuth(getAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null
                || !(response.get("join_url") instanceof String joinUrl) || joinUrl.isBlank()) {
            throw new IllegalStateException("Zoom did not return a meeting ID and join URL");
        }

        return new ZoomMeetingDetails(response.get("id").toString(), joinUrl);
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        Map<?, ?> response = restClient.post()
                .uri("https://zoom.us/oauth/token?grant_type=account_credentials&account_id={accountId}", accountId)
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("access_token") instanceof String token)
                || !(response.get("expires_in") instanceof Number expiresIn)) {
            throw new IllegalStateException("Zoom did not return a valid access token");
        }

        accessToken = token;
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(1, expiresIn.longValue() - 60));
        return accessToken;
    }

    private String newPasscode() {
        StringBuilder passcode = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            passcode.append(PASSCODE_CHARACTERS.charAt(random.nextInt(PASSCODE_CHARACTERS.length())));
        }
        return passcode.toString();
    }

    public record ZoomMeetingDetails(String id, String joinUrl) {
    }
}
