package com.taino.changerequest;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChangeRequestDtos {

    private ChangeRequestDtos() {
    }

    /**
     * Either a free-form edit request (reason + requestedChanges)
     * or an add-item request (addTitle, optional url/image).
     */
    public record CreateRequest(
            @Size(max = 1000) String reason,
            @Size(max = 2000) String requestedChanges,
            @Size(max = 200) String addTitle,
            @Size(max = 2000) String addUrl,
            UUID addImageId
    ) {
    }

    public record DecideRequest(
            boolean approve,
            @Size(max = 1000) String adminNote,
            Integer editWindowHours
    ) {
    }

    public record ChangeRequestResponse(
            Long id,
            Long memberId,
            String displayName,
            String reason,
            String requestedChanges,
            String status,
            String adminNote,
            Instant createdAt,
            Instant resolvedAt
    ) {
    }

    public record ListResponse(List<ChangeRequestResponse> items) {
    }
}
