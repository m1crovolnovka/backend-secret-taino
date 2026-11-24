package com.taino.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record StaffLoginRequest(
            @NotBlank @Size(max = 64) String login,
            @NotBlank @Size(max = 128) String password
    ) {
    }

    public record StaffChangePasswordRequest(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(min = 6, max = 128) String newPassword,
            @Size(max = 64) String newLogin
    ) {
    }

    public record StaffAuthResponse(
            String accessToken,
            long expiresIn,
            StaffMe me
    ) {
    }

    public record StaffMe(
            Long id,
            String login,
            boolean mustChangePassword
    ) {
    }

    public record TrackEventRequest(
            @NotBlank @Size(max = 48) String type,
            @Size(max = 32) String code,
            @Size(max = 64) String visitorKey,
            @Size(max = 1000) String referrer,
            Map<String, Object> meta
    ) {
    }

    public record TrackClickResponse(
            String code,
            String redirectUrl,
            String telegramUrl
    ) {
    }

    public record CreateLinkRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 32) String code,
            @Size(max = 120) String utmSource,
            @Size(max = 120) String utmMedium,
            @Size(max = 160) String utmCampaign,
            @Size(max = 160) String utmContent,
            @Size(max = 160) String utmTerm,
            @Size(max = 160) String label
    ) {
    }

    public record UpdateLinkRequest(
            @Size(max = 160) String title,
            @Size(max = 120) String utmSource,
            @Size(max = 120) String utmMedium,
            @Size(max = 160) String utmCampaign,
            @Size(max = 160) String utmContent,
            @Size(max = 160) String utmTerm,
            @Size(max = 160) String label,
            Boolean active
    ) {
    }

    public record LinkResponse(
            Long id,
            String code,
            String title,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            String utmContent,
            String utmTerm,
            String label,
            boolean active,
            String startAppParam,
            String telegramUrl,
            String trackUrl,
            Instant createdAt,
            long clicks,
            long opens,
            long users
    ) {
    }

    public record OverviewResponse(
            Instant from,
            Instant to,
            long linkClicks,
            long uniqueClickVisitors,
            long miniAppOpens,
            long uniqueOpenUsers,
            long registrations,
            long logins,
            long roomsCreated,
            long roomJoins,
            long totalUsers,
            long activeParticipants,
            long roomsTotal,
            long roomsDrawn
    ) {
    }

    public record FunnelStep(String id, String label, long value, Double conversionFromPrev) {
    }

    public record FunnelResponse(Instant from, Instant to, List<FunnelStep> steps) {
    }

    public record TimeseriesPoint(String day, String eventType, long count) {
    }

    public record TimeseriesResponse(Instant from, Instant to, List<TimeseriesPoint> points) {
    }

    public record SourceRow(
            String source,
            String medium,
            String campaign,
            String code,
            String title,
            long clicks,
            long opens,
            long users
    ) {
    }

    public record SourcesResponse(Instant from, Instant to, List<SourceRow> rows, List<ReferrerRow> referrers) {
    }

    public record ReferrerRow(String referrer, long count) {
    }
}
