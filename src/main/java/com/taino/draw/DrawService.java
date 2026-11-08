package com.taino.draw;

import com.taino.audit.AuditService;
import com.taino.common.ApiException;
import com.taino.form.FormEntity;
import com.taino.form.FormRepository;
import com.taino.notification.NotificationService;
import com.taino.room.MemberStatus;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomEntity;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomMemberRepository;
import com.taino.room.RoomPhase;
import com.taino.room.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
public class DrawService {

    public static final int MIN_PARTICIPANTS = 3;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final AssignmentRepository assignmentRepository;
    private final FormRepository formRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final RoomAccessService access;
    private final SecureRandom random = new SecureRandom();

    public DrawService(
            RoomRepository roomRepository,
            RoomMemberRepository memberRepository,
            AssignmentRepository assignmentRepository,
            FormRepository formRepository,
            NotificationService notificationService,
            AuditService auditService,
            RoomAccessService access
    ) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.formRepository = formRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.access = access;
    }

    /**
     * Idempotent draw under row lock. Returns true if draw performed.
     */
    @Transactional
    public boolean drawRoom(Long roomId, Long actorUserId, boolean force) {
        RoomEntity room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Комната не найдена"));

        if (room.isDrawLocked() && room.getPhase() != RoomPhase.REGISTRATION && !force) {
            return false;
        }
        if (!force && room.getPhase() != RoomPhase.REGISTRATION) {
            return false;
        }
        if (!force && assignmentRepository.existsByRoomId(roomId)) {
            room.setDrawLocked(true);
            return false;
        }

        List<RoomMemberEntity> participants = memberRepository.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE)
                .stream()
                .filter(RoomMemberEntity::isParticipates)
                .toList();

        if (participants.size() < MIN_PARTICIPANTS) {
            notificationService.notifyAdminDeferredDraw(room,
                    "Нужно минимум " + MIN_PARTICIPANTS + " участников, сейчас " + participants.size());
            auditService.record(actorUserId, roomId, "DRAW_DEFERRED", Map.of("count", participants.size()));
            return false;
        }

        if (force && assignmentRepository.existsByRoomId(roomId)) {
            assignmentRepository.deleteByRoomId(roomId);
            assignmentRepository.flush();
        }

        int[] perm = Derangement.cycle(participants.size(), random);
        if (!Derangement.isValid(perm)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DRAW_FAILED", "Сбой алгоритма раздачи");
        }

        for (int i = 0; i < participants.size(); i++) {
            AssignmentEntity a = new AssignmentEntity();
            a.setRoom(room);
            a.setGiver(participants.get(i));
            a.setReceiver(participants.get(perm[i]));
            assignmentRepository.save(a);
        }

        for (RoomMemberEntity m : participants) {
            formRepository.findByMemberId(m.getId()).ifPresent(f -> {
                f.setLocked(true);
                m.setEditWindowUntil(null);
            });
        }

        room.setPhase(RoomPhase.DRAW_DONE);
        room.setDrawLocked(true);
        notificationService.notifyDrawDone(room);
        auditService.record(actorUserId, roomId, force ? "DRAW_REDRAW" : "DRAW_DONE",
                Map.of("participants", participants.size()));
        return true;
    }

    @Transactional
    public boolean adminDraw(Long userId, Long roomId, boolean redraw) {
        access.requireAdminTab(roomId, userId);
        RoomEntity room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Комната не найдена"));
        if (!redraw && room.isDrawLocked()) {
            throw new ApiException(HttpStatus.CONFLICT, "DRAW_LOCKED", "Раздача уже выполнена");
        }
        if (redraw) {
            assignmentRepository.deleteByRoomId(roomId);
            assignmentRepository.flush();
            room.setDrawLocked(false);
            room.setPhase(RoomPhase.REGISTRATION);
        }
        boolean ok = drawRoom(roomId, userId, redraw);
        if (!ok) {
            throw new ApiException(HttpStatus.CONFLICT, "DRAW_FAILED",
                    "Раздача не выполнена (мало участников или уже есть результат)");
        }
        return true;
    }
}
