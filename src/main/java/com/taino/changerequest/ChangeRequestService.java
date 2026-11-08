package com.taino.changerequest;

import com.taino.audit.AuditService;
import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import com.taino.form.FormRepository;
import com.taino.notification.NotificationService;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomPhase;
import com.taino.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class ChangeRequestService {

    private final ChangeRequestRepository repository;
    private final FormRepository formRepository;
    private final RoomAccessService access;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public ChangeRequestService(
            ChangeRequestRepository repository,
            FormRepository formRepository,
            RoomAccessService access,
            NotificationService notificationService,
            AuditService auditService,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.formRepository = formRepository;
        this.access = access;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChangeRequestDtos.ChangeRequestResponse create(Long userId, Long roomId, ChangeRequestDtos.CreateRequest req) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        if (!member.isParticipates()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Вы не участвуете");
        }
        if (member.getRoom().getPhase() == RoomPhase.REGISTRATION
                && access.canEditOwnContent(member.getRoom(), member)) {
            throw new ApiException(HttpStatus.CONFLICT, "EDIT_OPEN", "Сейчас можно редактировать без запроса");
        }
        if (repository.existsByMemberIdAndStatus(member.getId(), "PENDING")) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_PENDING", "Уже есть открытый запрос");
        }
        ChangeRequestEntity entity = new ChangeRequestEntity();
        entity.setRoom(member.getRoom());
        entity.setMember(member);
        entity.setReason(TextSanitizer.requirePlain(req.reason(), 1000, "Причина"));
        entity.setRequestedChanges(TextSanitizer.requirePlain(req.requestedChanges(), 2000, "Что изменить"));
        entity.setStatus("PENDING");
        entity = repository.save(entity);
        auditService.record(userId, roomId, "CHANGE_REQUEST_CREATED", Map.of("id", entity.getId()));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public ChangeRequestDtos.ListResponse listMine(Long userId, Long roomId) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        List<ChangeRequestDtos.ChangeRequestResponse> items = repository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .filter(r -> r.getMember().getId().equals(member.getId()))
                .map(this::toDto)
                .toList();
        return new ChangeRequestDtos.ListResponse(items);
    }

    @Transactional(readOnly = true)
    public ChangeRequestDtos.ListResponse listAdmin(Long userId, Long roomId) {
        access.requireAdminTab(roomId, userId);
        return new ChangeRequestDtos.ListResponse(
                repository.findByRoomIdOrderByCreatedAtDesc(roomId).stream().map(this::toDto).toList()
        );
    }

    @Transactional
    public ChangeRequestDtos.ChangeRequestResponse decide(
            Long adminUserId, Long roomId, Long requestId, ChangeRequestDtos.DecideRequest req
    ) {
        access.requireAdminTab(roomId, adminUserId);
        ChangeRequestEntity entity = repository.findByIdAndRoomId(requestId, roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Запрос не найден"));
        if (!"PENDING".equals(entity.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_RESOLVED", "Запрос уже обработан");
        }
        entity.setStatus(req.approve() ? "APPROVED" : "REJECTED");
        entity.setAdminNote(TextSanitizer.plain(req.adminNote(), 1000));
        entity.setResolvedAt(Instant.now());
        entity.setResolvedBy(userRepository.findById(adminUserId).orElse(null));

        RoomMemberEntity member = entity.getMember();
        if (req.approve()) {
            int hours = req.editWindowHours() != null && req.editWindowHours() > 0 ? req.editWindowHours() : 2;
            member.setEditWindowUntil(Instant.now().plus(hours, ChronoUnit.HOURS));
            formRepository.findByMemberId(member.getId()).ifPresent(f -> f.setLocked(false));
        }
        notificationService.notifyChangeDecision(member.getUser().getId(), roomId, req.approve(), entity.getAdminNote());
        auditService.record(adminUserId, roomId, req.approve() ? "CHANGE_APPROVED" : "CHANGE_REJECTED",
                Map.of("id", requestId));
        return toDto(entity);
    }

    private ChangeRequestDtos.ChangeRequestResponse toDto(ChangeRequestEntity e) {
        String display = formRepository.findByMemberId(e.getMember().getId())
                .map(f -> f.getDisplayName())
                .orElse(e.getMember().getUser().getFirstName());
        return new ChangeRequestDtos.ChangeRequestResponse(
                e.getId(),
                e.getMember().getId(),
                display,
                e.getReason(),
                e.getRequestedChanges(),
                e.getStatus(),
                e.getAdminNote(),
                e.getCreatedAt(),
                e.getResolvedAt()
        );
    }
}
