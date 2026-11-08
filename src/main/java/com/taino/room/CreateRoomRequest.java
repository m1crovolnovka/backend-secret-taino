package com.taino.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateRoomRequest(
        @NotBlank @Size(min = 2, max = 120) String title,
        @NotBlank @Size(max = 32) String theme,
        @NotNull AdminMode adminMode,
        @NotNull Instant registrationEndsAt,
        @NotNull Instant deliveryAt,
        @Size(max = 500) String deliveryPlace,
        @Min(0) @Max(10_000_000) int budgetMin,
        @Min(0) @Max(10_000_000) int budgetMax,
        @NotBlank @Size(max = 8) String currency
) {
}
