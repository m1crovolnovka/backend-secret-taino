package com.taino.room;

import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import com.taino.config.TainoProperties;
import com.taino.form.FormEntity;
import com.taino.form.FormRepository;
import com.taino.notification.NotificationService;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RoomService {

    private static final Set<String> CURRENCIES = Set.of("BYN", "RUB", "USD", "EUR");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final RoomCodeGenerator codeGenerator;
    private final RoomAccessService access;
    private final NotificationService notificationService;
    private final TainoProperties properties;

    public RoomService(
            RoomRepository roomRepository,
            RoomMemberRepository memberRepository,
            FormRepository formRepository,
            UserRepository userRepository,
            RoomCodeGenerator codeGenerator,
            RoomAccessService access,
            NotificationService notificationService,
            TainoProperties properties
    ) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.codeGenerator = codeGenerator;
        this.access = access;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<RoomDtos.RoomSummary> listMyRooms(Long userId) {
        return memberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE).stream()
                .map(m -> toSummary(m.getRoom(), m))
                .toList();
    }

    @Transactional
    public RoomDtos.RoomDetail create(Long userId, CreateRoomRequest req) {
        UserEntity owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Пользователь не найден"));

        validateDates(req.registrationEndsAt(), req.deliveryAt());
        validateBudget(req.budgetMin(), req.budgetMax());
        String theme = requireTheme(req.theme());
        String currency = requireCurrency(req.currency());
        String title = TextSanitizer.requirePlain(req.title(), 120, "Название");
        String place = TextSanitizer.plain(req.deliveryPlace(), 500);

        RoomEntity room = new RoomEntity();
        room.setCode(uniqueCode());
        room.setTitle(title);
        room.setTheme(theme);
        room.setPhase(RoomPhase.REGISTRATION);
        room.setAdminMode(req.adminMode());
        room.setOwner(owner);
        room.setRegistrationEndsAt(req.registrationEndsAt());
        room.setDeliveryAt(req.deliveryAt());
        room.setDeliveryPlace(place);
        room.setBudgetMin(req.budgetMin());
        room.setBudgetMax(req.budgetMax());
        room.setCurrency(currency);
        room = roomRepository.save(room);

        RoomMemberEntity ownerMember = addMember(room, owner, MemberRole.OWNER, req.adminMode().ownerParticipates());
        ensureForm(ownerMember, owner);
        notificationService.scheduleRoomLifecycle(room);

        return toDetail(room, ownerMember);
    }

    @Transactional
    public RoomDtos.RoomDetail join(Long userId, JoinRoomRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Пользователь не найден"));

        RoomEntity room = roomRepository.findByCodeIgnoreCase(req.code().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Комната не найдена"));

        if (room.getPhase() != RoomPhase.REGISTRATION) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_CLOSED", "В эту комнату уже нельзя войти");
        }

        var existing = memberRepository.findByRoomIdAndUserId(room.getId(), userId);
        if (existing.isPresent()) {
            RoomMemberEntity m = existing.get();
            if (m.getStatus() == MemberStatus.KICKED) {
                throw new ApiException(HttpStatus.FORBIDDEN, "KICKED", "Вас исключили из комнаты");
            }
            return toDetail(room, m);
        }

        RoomMemberEntity member = addMember(room, user, MemberRole.MEMBER, true);
        ensureForm(member, user);
        notificationService.enqueue(userId, room.getId(), "ROOM_JOINED", Instant.now(),
                Map.of("title", room.getTitle()));
        return toDetail(room, member);
    }

    @Transactional(readOnly = true)
    public RoomDtos.RoomDetail get(Long userId, Long roomId) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        return toDetail(member.getRoom(), member);
    }

    @Transactional
    public RoomDtos.RoomDetail update(Long userId, Long roomId, UpdateRoomRequest req) {
        RoomMemberEntity ownerMember = access.requireOwner(roomId, userId);
        RoomEntity room = ownerMember.getRoom();

        if (StringUtils.hasText(req.title())) {
            room.setTitle(TextSanitizer.requirePlain(req.title(), 120, "Название"));
        }
        if (StringUtils.hasText(req.theme())) {
            room.setTheme(requireTheme(req.theme()));
        }
        if (req.adminMode() != null) {
            room.setAdminMode(req.adminMode());
            ownerMember.setParticipates(req.adminMode().ownerParticipates());
        }
        if (req.registrationEndsAt() != null) {
            Instant delivery = req.deliveryAt() != null ? req.deliveryAt() : room.getDeliveryAt();
            validateDates(req.registrationEndsAt(), delivery);
            room.setRegistrationEndsAt(req.registrationEndsAt());
        }
        if (req.deliveryAt() != null) {
            validateDates(room.getRegistrationEndsAt(), req.deliveryAt());
            room.setDeliveryAt(req.deliveryAt());
        }
        if (req.deliveryPlace() != null) {
            room.setDeliveryPlace(TextSanitizer.plain(req.deliveryPlace(), 500));
        }
        int min = req.budgetMin() != null ? req.budgetMin() : room.getBudgetMin();
        int max = req.budgetMax() != null ? req.budgetMax() : room.getBudgetMax();
        validateBudget(min, max);
        room.setBudgetMin(min);
        room.setBudgetMax(max);
        if (StringUtils.hasText(req.currency())) {
            room.setCurrency(requireCurrency(req.currency()));
        }

        return toDetail(room, ownerMember);
    }

    @Transactional(readOnly = true)
    public RoomDtos.AdminParticipantsResponse adminParticipants(Long userId, Long roomId) {
        access.requireAdminTab(roomId, userId);
        List<RoomDtos.AdminParticipant> list = new ArrayList<>();
        for (RoomMemberEntity m : memberRepository.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE)) {
            FormEntity form = formRepository.findByMemberId(m.getId()).orElse(null);
            list.add(new RoomDtos.AdminParticipant(
                    m.getId(),
                    m.getUser().getId(),
                    m.getUser().getUsername(),
                    m.getUser().getFirstName(),
                    form != null ? form.getDisplayName() : m.getUser().getFirstName(),
                    m.getColorCode(),
                    m.isParticipates(),
                    m.getRole(),
                    m.getStatus()
            ));
        }
        return new RoomDtos.AdminParticipantsResponse(list);
    }

    private RoomMemberEntity addMember(RoomEntity room, UserEntity user, MemberRole role, boolean participates) {
        RoomMemberEntity member = new RoomMemberEntity();
        member.setRoom(room);
        member.setUser(user);
        member.setRole(role);
        member.setParticipates(participates);
        member.setStatus(MemberStatus.ACTIVE);
        if (participates) {
            member.setColorCode(pickColor(room));
        }
        return memberRepository.save(member);
    }

    private void ensureForm(RoomMemberEntity member, UserEntity user) {
        if (formRepository.findByMemberId(member.getId()).isPresent()) {
            return;
        }
        FormEntity form = new FormEntity();
        form.setMember(member);
        form.setDisplayName(TextSanitizer.requirePlain(user.getFirstName(), 120, "Имя"));
        form.setLocked(false);
        formRepository.save(form);
    }

    private String pickColor(RoomEntity room) {
        Set<String> used = new HashSet<>(memberRepository.findUsedColors(room.getId()));
        List<String> palette = ColorPalette.forTheme(room.getTheme());
        List<String> free = palette.stream().filter(c -> !used.contains(c)).toList();
        if (free.isEmpty()) {
            // overflow: generate unique suffix
            String base = palette.get(RANDOM.nextInt(palette.size()));
            return base + "-" + Long.toString(System.nanoTime(), 36);
        }
        return free.get(RANDOM.nextInt(free.size()));
    }

    private String uniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = codeGenerator.next();
            if (!roomRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_GEN", "Не удалось создать код комнаты");
    }

    private static void validateDates(Instant registrationEnds, Instant delivery) {
        Instant now = Instant.now();
        if (registrationEnds == null || delivery == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Нужны обе даты");
        }
        if (!registrationEnds.isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Конец заполнения должен быть в будущем");
        }
        if (!delivery.isAfter(registrationEnds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Вручение должно быть после раздачи");
        }
    }

    private static void validateBudget(int min, int max) {
        if (min < 0 || max < min) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Некорректный бюджет");
        }
    }

    private static String requireTheme(String theme) {
        String t = theme.trim().toLowerCase(Locale.ROOT);
        if (!ColorPalette.isKnownTheme(t)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Неизвестная тема");
        }
        return t;
    }

    private static String requireCurrency(String currency) {
        String c = currency.trim().toUpperCase(Locale.ROOT);
        if (!CURRENCIES.contains(c)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Неподдерживаемая валюта");
        }
        return c;
    }

    private RoomDtos.RoomDetail toDetail(RoomEntity room, RoomMemberEntity member) {
        boolean owner = member.getRole() == MemberRole.OWNER;
        boolean hasAdminTab = owner && room.getAdminMode().hasAdminTab();
        FormEntity form = formRepository.findByMemberId(member.getId()).orElse(null);
        return new RoomDtos.RoomDetail(
                toSummary(room, member),
                new RoomDtos.MembershipMe(
                        member.getId(),
                        member.isParticipates(),
                        form != null && form.isLocked(),
                        member.getEditWindowUntil(),
                        access.canEditOwnContent(room, member)
                )
        );
    }

    private RoomDtos.RoomSummary toSummary(RoomEntity room, RoomMemberEntity member) {
        boolean owner = member.getRole() == MemberRole.OWNER;
        long count = memberRepository.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE);
        return new RoomDtos.RoomSummary(
                room.getId(),
                room.getCode(),
                room.getTitle(),
                room.getTheme(),
                room.getPhase(),
                room.getAdminMode(),
                room.getRegistrationEndsAt(),
                room.getDeliveryAt(),
                room.getDeliveryPlace(),
                room.getBudgetMin(),
                room.getBudgetMax(),
                room.getCurrency(),
                room.isDrawLocked(),
                member.getRole().name(),
                member.isParticipates(),
                owner && room.getAdminMode().hasAdminTab(),
                owner,
                count,
                inviteLink(room.getCode())
        );
    }

    private String inviteLink(String code) {
        String username = properties.getTelegram().getBotUsername();
        if (username != null && !username.isBlank()) {
            String bot = username.startsWith("@") ? username.substring(1) : username;
            return "https://t.me/" + bot + "?start=room_" + code;
        }
        // Fallback: open Mini App directly with start param (still joins the room on boot).
        String mini = properties.getMiniAppUrl();
        if (mini == null || mini.isBlank()) {
            return "";
        }
        String sep = mini.contains("?") ? "&" : "?";
        return mini + sep + "startapp=room_" + code;
    }
}
