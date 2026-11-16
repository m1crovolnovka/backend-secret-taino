package com.taino.room;

import jakarta.validation.constraints.NotNull;

public record JoinPublicRoomRequest(
        @NotNull Long roomId
) {
}
