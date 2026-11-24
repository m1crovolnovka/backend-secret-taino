package com.taino.form;

import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomEntity;
import com.taino.room.RoomMemberEntity;
import com.taino.room.SecrecyMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FormService {

    private final FormRepository formRepository;
    private final RoomAccessService access;

    public FormService(FormRepository formRepository, RoomAccessService access) {
        this.formRepository = formRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public FormDtos.FormResponse getMine(Long userId, Long roomId) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        FormEntity form = requireForm(member.getId());
        return toDto(form, member.getRoom(), access.canEditOwnContent(member.getRoom(), member));
    }

    @Transactional
    public FormDtos.FormResponse updateMine(Long userId, Long roomId, FormDtos.UpdateFormRequest req) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        if (!member.isParticipates()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Вы не участвуете в раздаче");
        }
        if (!access.canEditOwnContent(member.getRoom(), member)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORM_LOCKED", "Форма закрыта. Запросите изменение у админа");
        }
        FormEntity form = requireForm(member.getId());
        applyCommon(form, req);
        if (isOpen(member.getRoom())) {
            if (req.wbPickup() != null) {
                form.setWbPickup(TextSanitizer.plain(req.wbPickup(), 500));
            }
            if (req.telegramLink() != null) {
                form.setTelegramLink(sanitizeTelegramLink(req.telegramLink()));
            }
        }
        return toDto(form, member.getRoom(), true);
    }

    @Transactional(readOnly = true)
    public FormDtos.FormResponse adminGet(Long userId, Long roomId, Long memberId) {
        access.requireAdminTab(roomId, userId);
        FormEntity form = requireForm(memberId);
        if (!form.getMember().getRoom().getId().equals(roomId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "Форма не найдена");
        }
        return toDto(form, form.getMember().getRoom(), true);
    }

    @Transactional
    public FormDtos.FormResponse adminUpdate(Long userId, Long roomId, Long memberId, FormDtos.UpdateFormRequest req) {
        access.requireAdminTab(roomId, userId);
        FormEntity form = requireForm(memberId);
        RoomEntity room = form.getMember().getRoom();
        if (!room.getId().equals(roomId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "Форма не найдена");
        }
        applyCommon(form, req);
        if (isOpen(room)) {
            if (req.wbPickup() != null) {
                form.setWbPickup(TextSanitizer.plain(req.wbPickup(), 500));
            }
            if (req.telegramLink() != null) {
                form.setTelegramLink(sanitizeTelegramLink(req.telegramLink()));
            }
        }
        return toDto(form, room, true);
    }

    private static void applyCommon(FormEntity form, FormDtos.UpdateFormRequest req) {
        if (StringUtils.hasText(req.displayName())) {
            form.setDisplayName(TextSanitizer.requirePlain(req.displayName(), 120, "Имя"));
        }
        if (req.note() != null) {
            form.setNote(TextSanitizer.plainKeepSpaces(req.note(), 2000));
        }
        if (req.clothingSize() != null) {
            form.setClothingSize(TextSanitizer.plain(req.clothingSize(), 32));
        }
        if (req.shoeSize() != null) {
            form.setShoeSize(TextSanitizer.plain(req.shoeSize(), 32));
        }
    }

    private FormEntity requireForm(Long memberId) {
        return formRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "Форма не найдена"));
    }

    private static boolean isOpen(RoomEntity room) {
        return room.getSecrecyMode() == SecrecyMode.OPEN;
    }

    private static String sanitizeTelegramLink(String raw) {
        String value = TextSanitizer.plain(raw, 500);
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("@")) {
            return "https://t.me/" + trimmed.substring(1);
        }
        if (trimmed.startsWith("t.me/")) {
            return "https://" + trimmed;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return TextSanitizer.httpUrl(trimmed);
        }
        if (trimmed.matches("[A-Za-z0-9_]{3,64}")) {
            return "https://t.me/" + trimmed;
        }
        return TextSanitizer.httpUrl(trimmed);
    }

    private static FormDtos.FormResponse toDto(FormEntity form, RoomEntity room, boolean canEdit) {
        boolean open = isOpen(room);
        return new FormDtos.FormResponse(
                form.getId(),
                form.getDisplayName(),
                form.getNote(),
                form.getClothingSize(),
                form.getShoeSize(),
                open ? form.getWbPickup() : null,
                open ? form.getTelegramLink() : null,
                open,
                form.isLocked(),
                canEdit
        );
    }
}
