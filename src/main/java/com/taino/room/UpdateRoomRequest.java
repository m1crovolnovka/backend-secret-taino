package com.taino.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateRoomRequest(
        @Size(min = 2, max = 120) String title,
        @Size(max = 32) String theme,
        AdminMode adminMode,
        Instant registrationEndsAt,
        Instant deliveryAt,
        @Size(max = 500) String deliveryPlace,
        @Min(0) @Max(10_000_000) Integer budgetMin,
        @Min(0) @Max(10_000_000) Integer budgetMax,
        @Size(max = 8) String currency
) {
}
