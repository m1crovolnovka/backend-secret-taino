package com.taino.room;

import java.time.Instant;
import java.util.List;

public final class RoomDtos {

    private RoomDtos() {
    }

    public record RoomSummary(
            Long id,
            String code,
            String title,
            String theme,
            RoomPhase phase,
            AdminMode adminMode,
            Instant registrationEndsAt,
            Instant deliveryAt,
            String deliveryPlace,
            int budgetMin,
            int budgetMax,
            String currency,
            boolean drawLocked,
            String myRole,
            boolean participates,
            boolean hasAdminTab,
            boolean canTune,
            long memberCount,
            String inviteLink,
            boolean archived
    ) {
    }

    public record RoomDetail(
            RoomSummary room,
            MembershipMe me
    ) {
    }

    public record MembershipMe(
            Long memberId,
            boolean participates,
            boolean formLocked,
            Instant editWindowUntil,
            boolean canEdit
    ) {
    }

    public record AdminParticipant(
            Long memberId,
            Long userId,
            String username,
            String firstName,
            String displayName,
            String colorCode,
            boolean participates,
            MemberRole role,
            MemberStatus status
    ) {
    }

    public record AdminParticipantsResponse(List<AdminParticipant> participants) {
    }
}
