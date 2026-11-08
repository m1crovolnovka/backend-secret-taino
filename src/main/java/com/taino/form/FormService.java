package com.taino.form;

import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomMemberEntity;
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
        return toDto(form, access.canEditOwnContent(member.getRoom(), member));
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
        if (StringUtils.hasText(req.displayName())) {
            form.setDisplayName(TextSanitizer.requirePlain(req.displayName(), 120, "Имя"));
        }
        if (req.note() != null) {
            form.setNote(TextSanitizer.plain(req.note(), 2000));
        }
        if (req.clothingSize() != null) {
            form.setClothingSize(TextSanitizer.plain(req.clothingSize(), 32));
        }
        if (req.shoeSize() != null) {
            form.setShoeSize(TextSanitizer.plain(req.shoeSize(), 32));
        }
        return toDto(form, true);
    }

    @Transactional(readOnly = true)
    public FormDtos.FormResponse adminGet(Long userId, Long roomId, Long memberId) {
        access.requireAdminTab(roomId, userId);
        FormEntity form = requireForm(memberId);
        if (!form.getMember().getRoom().getId().equals(roomId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "Форма не найдена");
        }
        return toDto(form, true);
    }

    @Transactional
    public FormDtos.FormResponse adminUpdate(Long userId, Long roomId, Long memberId, FormDtos.UpdateFormRequest req) {
        access.requireAdminTab(roomId, userId);
        FormEntity form = requireForm(memberId);
        if (!form.getMember().getRoom().getId().equals(roomId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "Форма не найдена");
        }
        if (StringUtils.hasText(req.displayName())) {
            form.setDisplayName(TextSanitizer.requirePlain(req.displayName(), 120, "Имя"));
        }
        if (req.note() != null) {
            form.setNote(TextSanitizer.plain(req.note(), 2000));
        }
        if (req.clothingSize() != null) {
            form.setClothingSize(TextSanitizer.plain(req.clothingSize(), 32));
        }
        if (req.shoeSize() != null) {
            form.setShoeSize(TextSanitizer.plain(req.shoeSize(), 32));
        }
        return toDto(form, true);
    }

    private FormEntity requireForm(Long memberId) {
        return formRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "Форма не найдена"));
    }

    private static FormDtos.FormResponse toDto(FormEntity form, boolean canEdit) {
        return new FormDtos.FormResponse(
                form.getId(),
                form.getDisplayName(),
                form.getNote(),
                form.getClothingSize(),
                form.getShoeSize(),
                form.isLocked(),
                canEdit
        );
    }
}
