package com.taino.room;

import com.taino.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomAccessService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    public RoomAccessService(RoomRepository roomRepository, RoomMemberRepository memberRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public RoomEntity requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Комната не найдена"));
    }

    @Transactional(readOnly = true)
    public RoomMemberEntity requireActiveMember(Long roomId, Long userId) {
        RoomMemberEntity member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NOT_A_MEMBER", "Вы не в этой комнате"));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_A_MEMBER", "Вы не в этой комнате");
        }
        return member;
    }

    /** Owner can tune settings in all admin modes, including ADMIN_AS_USER. */
    @Transactional(readOnly = true)
    public RoomMemberEntity requireOwner(Long roomId, Long userId) {
        RoomMemberEntity member = requireActiveMember(roomId, userId);
        if (member.getRole() != MemberRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Только владелец комнаты");
        }
        return member;
    }

    /**
     * Admin tab (colors, others' forms). Blocked for ADMIN_AS_USER.
     */
    @Transactional(readOnly = true)
    public RoomMemberEntity requireAdminTab(Long roomId, Long userId) {
        RoomMemberEntity member = requireOwner(roomId, userId);
        if (!member.getRoom().getAdminMode().hasAdminTab()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_AS_USER", "В режиме «как участник» админ-вкладка закрыта");
        }
        return member;
    }

    public boolean canEditOwnContent(RoomEntity room, RoomMemberEntity member) {
        if (room.getPhase() == RoomPhase.REGISTRATION
                && room.getRegistrationEndsAt().isAfter(java.time.Instant.now())) {
            return true;
        }
        java.time.Instant until = member.getEditWindowUntil();
        return until != null && until.isAfter(java.time.Instant.now());
    }
}
