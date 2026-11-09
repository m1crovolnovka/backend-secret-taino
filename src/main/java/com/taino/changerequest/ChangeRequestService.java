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
import com.taino.wishlist.WishlistDtos;
import com.taino.wishlist.WishlistItemType;
import com.taino.wishlist.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangeRequestService {

    private static final String ADD_PREFIX = "ADD_ITEM|";

    private final ChangeRequestRepository repository;
    private final FormRepository formRepository;
    private final RoomAccessService access;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final WishlistService wishlistService;

    public ChangeRequestService(
            ChangeRequestRepository repository,
            FormRepository formRepository,
            RoomAccessService access,
            NotificationService notificationService,
            AuditService auditService,
            UserRepository userRepository,
            WishlistService wishlistService
    ) {
        this.repository = repository;
        this.formRepository = formRepository;
        this.access = access;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.wishlistService = wishlistService;
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

        boolean addItem = StringUtils.hasText(req.addTitle());
        String reason;
        String changes;
        if (addItem) {
            String title = TextSanitizer.requirePlain(req.addTitle(), 200, "Название");
            String url = req.addUrl() != null ? TextSanitizer.plain(req.addUrl(), 2000) : "";
            String imageId = req.addImageId() != null ? req.addImageId().toString() : "";
            reason = "Добавить желание";
            changes = ADD_PREFIX + title + "|" + nullToEmpty(url) + "|" + imageId;
        } else {
            if (!StringUtils.hasText(req.reason()) || !StringUtils.hasText(req.requestedChanges())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Нужна причина и что поменять");
            }
            reason = TextSanitizer.requirePlain(req.reason(), 1000, "Причина");
            changes = TextSanitizer.requirePlain(req.requestedChanges(), 2000, "Что изменить");
        }

        ChangeRequestEntity entity = new ChangeRequestEntity();
        entity.setRoom(member.getRoom());
        entity.setMember(member);
        entity.setReason(reason);
        entity.setRequestedChanges(changes);
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
            if (entity.getRequestedChanges() != null && entity.getRequestedChanges().startsWith(ADD_PREFIX)) {
                applyAddItem(adminUserId, member, entity.getRequestedChanges());
            } else {
                int hours = req.editWindowHours() != null && req.editWindowHours() > 0 ? req.editWindowHours() : 2;
                member.setEditWindowUntil(Instant.now().plus(hours, ChronoUnit.HOURS));
                formRepository.findByMemberId(member.getId()).ifPresent(f -> f.setLocked(false));
            }
        }
        notificationService.notifyChangeDecision(member.getUser().getId(), roomId, req.approve(), entity.getAdminNote());
        auditService.record(adminUserId, roomId, req.approve() ? "CHANGE_APPROVED" : "CHANGE_REJECTED",
                Map.of("id", requestId));
        return toDto(entity);
    }

    private void applyAddItem(Long adminUserId, RoomMemberEntity member, String encoded) {
        String raw = encoded.substring(ADD_PREFIX.length());
        String[] parts = raw.split("\\|", 3);
        String title = parts.length > 0 ? parts[0] : "";
        String url = parts.length > 1 ? parts[1] : "";
        String imageId = parts.length > 2 ? parts[2] : "";
        if (!StringUtils.hasText(title)) {
            return;
        }
        WishlistItemType type;
        UUID img = null;
        if (StringUtils.hasText(imageId)) {
            type = WishlistItemType.IMAGE;
            img = UUID.fromString(imageId);
        } else if (StringUtils.hasText(url)) {
            type = WishlistItemType.LINK;
        } else {
            type = WishlistItemType.TEXT;
        }
        // Image was uploaded by the member — ownership check must use their user id.
        Long ownerUserId = member.getUser().getId();
        wishlistService.addForMember(
                ownerUserId,
                member,
                new WishlistDtos.UpsertItemRequest(type, title, null, StringUtils.hasText(url) ? url : null, null, img, null)
        );
    }

    private ChangeRequestDtos.ChangeRequestResponse toDto(ChangeRequestEntity e) {
        String display = formRepository.findByMemberId(e.getMember().getId())
                .map(f -> f.getDisplayName())
                .orElse(e.getMember().getUser().getFirstName());
        String changes = e.getRequestedChanges();
        if (changes != null && changes.startsWith(ADD_PREFIX)) {
            String[] parts = changes.substring(ADD_PREFIX.length()).split("\\|", 3);
            String title = parts.length > 0 ? parts[0] : "";
            String url = parts.length > 1 && StringUtils.hasText(parts[1]) ? parts[1] : "";
            changes = "Добавить: «" + title + "»" + (StringUtils.hasText(url) ? " · " + url : "");
        }
        return new ChangeRequestDtos.ChangeRequestResponse(
                e.getId(),
                e.getMember().getId(),
                display,
                e.getReason(),
                changes,
                e.getStatus(),
                e.getAdminNote(),
                e.getCreatedAt(),
                e.getResolvedAt()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
