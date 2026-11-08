package com.taino.room;

import com.taino.audit.AuditService;
import com.taino.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AdminMemberService {

    private final RoomMemberRepository memberRepository;
    private final RoomAccessService access;
    private final AuditService auditService;

    public AdminMemberService(
            RoomMemberRepository memberRepository,
            RoomAccessService access,
            AuditService auditService
    ) {
        this.memberRepository = memberRepository;
        this.access = access;
        this.auditService = auditService;
    }

    @Transactional
    public void kick(Long adminUserId, Long roomId, Long memberId) {
        access.requireAdminTab(roomId, adminUserId);
        RoomMemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Участник не найден"));
        if (!member.getRoom().getId().equals(roomId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Участник не найден");
        }
        if (member.getRole() == MemberRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_KICK_OWNER", "Нельзя исключить владельца");
        }
        if (member.getRoom().isDrawLocked()) {
            throw new ApiException(HttpStatus.CONFLICT, "DRAW_LOCKED", "После раздачи кик недоступен");
        }
        member.setStatus(MemberStatus.KICKED);
        member.setParticipates(false);
        auditService.record(adminUserId, roomId, "MEMBER_KICKED", Map.of("memberId", memberId));
    }
}
