package com.taino.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank @Size(min = 4, max = 16) String code
) {
}
