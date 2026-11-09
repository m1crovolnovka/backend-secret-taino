package com.taino.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taino.room.RoomEntity;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomMemberRepository;
import com.taino.room.MemberStatus;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final RoomMemberRepository memberRepository;
    private final TelegramBotClient botClient;
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationRepository repository,
            UserRepository userRepository,
            RoomMemberRepository memberRepository,
            TelegramBotClient botClient,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.botClient = botClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(Long userId, Long roomId, String type, Instant when, Map<String, ?> payload) {
        if (repository.existsByRoomIdAndUserIdAndTypeAndStatus(roomId, userId, type, "PENDING")
                || repository.existsByRoomIdAndUserIdAndTypeAndStatus(roomId, userId, type, "SENT")) {
            // allow reminders with distinct types only
            if (!type.contains("REMINDER") && !type.contains("CHANGE")) {
                return;
            }
        }
        NotificationEntity n = new NotificationEntity();
        n.setUserId(userId);
        n.setRoomId(roomId);
        n.setType(type);
        n.setScheduledAt(when);
        n.setStatus("PENDING");
        try {
            if (payload != null) {
                n.setPayloadJson(objectMapper.writeValueAsString(payload));
            }
        } catch (Exception ignored) {
            // ignore
        }
        repository.save(n);
    }

    @Transactional
    public void scheduleRoomLifecycle(RoomEntity room) {
        Instant reg = room.getRegistrationEndsAt();
        Instant del = room.getDeliveryAt();
        List<RoomMemberEntity> members = memberRepository.findByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE);
        for (RoomMemberEntity m : members) {
            Long uid = m.getUser().getId();
            enqueue(uid, room.getId(), "ROOM_JOINED", Instant.now(), Map.of("title", room.getTitle()));
            enqueue(uid, room.getId(), "REMINDER_REG_24H", reg.minus(24, ChronoUnit.HOURS), Map.of("title", room.getTitle()));
            enqueue(uid, room.getId(), "REMINDER_REG_1H", reg.minus(1, ChronoUnit.HOURS), Map.of("title", room.getTitle()));
            enqueue(uid, room.getId(), "REMINDER_DELIVERY", del.minus(24, ChronoUnit.HOURS), Map.of("title", room.getTitle()));
        }
    }

    @Transactional
    public void notifyDrawDone(RoomEntity room) {
        for (RoomMemberEntity m : memberRepository.findByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE)) {
            if (!m.isParticipates()) {
                continue;
            }
            enqueue(m.getUser().getId(), room.getId(), "DRAW_DONE", Instant.now(),
                    Map.of("title", room.getTitle()));
        }
        enqueue(room.getOwner().getId(), room.getId(), "DRAW_DONE_ADMIN", Instant.now(),
                Map.of("title", room.getTitle()));
    }

    @Transactional
    public void notifyChangeDecision(Long userId, Long roomId, boolean approved, String note) {
        enqueue(userId, roomId, approved ? "CHANGE_APPROVED" : "CHANGE_REJECTED", Instant.now(),
                Map.of("note", note == null ? "" : note));
    }

    @Transactional
    public void notifyAdminDeferredDraw(RoomEntity room, String reason) {
        enqueue(room.getOwner().getId(), room.getId(), "DRAW_DEFERRED", Instant.now(),
                Map.of("reason", reason, "title", room.getTitle()));
    }

    @Transactional
    public int processDue() {
        List<NotificationEntity> due = repository.findDue(Instant.now());
        int sent = 0;
        for (NotificationEntity n : due) {
            UserEntity user = userRepository.findById(n.getUserId()).orElse(null);
            if (user == null || user.getTelegramId() == null) {
                n.setStatus("FAILED");
                continue;
            }
            String text = render(n);
            boolean ok = botClient.sendMessage(user.getTelegramId(), text);
            n.setStatus(ok ? "SENT" : "FAILED");
            n.setSentAt(Instant.now());
            if (ok) {
                sent++;
            } else {
                log.debug("notification {} failed", n.getId());
            }
        }
        return sent;
    }

    private String render(NotificationEntity n) {
        String title = "";
        try {
            if (n.getPayloadJson() != null) {
                title = objectMapper.readTree(n.getPayloadJson()).path("title").asText("");
            }
        } catch (Exception ignored) {
            // ignore
        }
        return switch (n.getType()) {
            case "ROOM_JOINED" -> "Ты в комнате «" + title + "». Заполни форму и вишлист до конца отсчёта.";
            case "REMINDER_REG_24H" -> "Через 24 часа конец заполнения в «" + title + "».";
            case "REMINDER_REG_1H" -> "Через час конец заполнения в «" + title + "».";
            case "DRAW_DONE" -> "Раздача в «" + title + "» готова. Открой Mini App — тебе выпал анонимный список.";
            case "DRAW_DONE_ADMIN" -> "Раздача в «" + title + "» выполнена.";
            case "DRAW_DEFERRED" -> "Раздача в «" + title + "» отложена: мало участников.";
            case "CHANGE_APPROVED" -> "Админ одобрил запрос на изменение. Окно редактирования открыто.";
            case "CHANGE_REJECTED" -> "Админ отклонил запрос на изменение.";
            case "REMINDER_DELIVERY" -> "Скоро вручение подарков в «" + title + "».";
            case "ROOM_DELETED" -> "Комната «" + title + "» удалена.";
            default -> "Обновление по комнате «" + title + "».";
        };
    }
}
