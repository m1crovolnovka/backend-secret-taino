package com.taino.changerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ChangeRequestDtos {

    private ChangeRequestDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 1000) String reason,
            @NotBlank @Size(max = 2000) String requestedChanges
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
