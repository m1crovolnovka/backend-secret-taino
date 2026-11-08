package com.taino.wishlist;

import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import com.taino.image.ImageEntity;
import com.taino.image.ImageRepository;
import com.taino.room.MemberStatus;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    public static final int MAX_ITEMS = 12;

    private final WishlistItemRepository itemRepository;
    private final ImageRepository imageRepository;
    private final RoomMemberRepository memberRepository;
    private final RoomAccessService access;

    public WishlistService(
            WishlistItemRepository itemRepository,
            ImageRepository imageRepository,
            RoomMemberRepository memberRepository,
            RoomAccessService access
    ) {
        this.itemRepository = itemRepository;
        this.imageRepository = imageRepository;
        this.memberRepository = memberRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public WishlistDtos.WishlistResponse getMine(Long userId, Long roomId) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        return build(member, access.canEditOwnContent(member.getRoom(), member));
    }

    @Transactional
    public WishlistDtos.WishlistItemResponse addMine(Long userId, Long roomId, WishlistDtos.UpsertItemRequest req) {
        RoomMemberEntity member = requireEditableParticipant(userId, roomId);
        if (itemRepository.countByMemberId(member.getId()) >= MAX_ITEMS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WISHLIST_FULL", "Максимум " + MAX_ITEMS + " пунктов");
        }
        WishlistItemEntity item = new WishlistItemEntity();
        item.setMember(member);
        apply(item, req, userId);
        item.setSortOrder(req.sortOrder() != null ? req.sortOrder() : (int) itemRepository.countByMemberId(member.getId()));
        return toDto(itemRepository.save(item));
    }

    @Transactional
    public WishlistDtos.WishlistItemResponse updateMine(Long userId, Long roomId, Long itemId, WishlistDtos.UpsertItemRequest req) {
        RoomMemberEntity member = requireEditableParticipant(userId, roomId);
        WishlistItemEntity item = itemRepository.findByIdAndMemberId(itemId, member.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "Пункт не найден"));
        apply(item, req, userId);
        if (req.sortOrder() != null) {
            item.setSortOrder(req.sortOrder());
        }
        return toDto(item);
    }

    @Transactional
    public void deleteMine(Long userId, Long roomId, Long itemId) {
        RoomMemberEntity member = requireEditableParticipant(userId, roomId);
        WishlistItemEntity item = itemRepository.findByIdAndMemberId(itemId, member.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "Пункт не найден"));
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public WishlistDtos.WishlistResponse adminGet(Long userId, Long roomId, Long memberId) {
        access.requireAdminTab(roomId, userId);
        RoomMemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Участник не найден"));
        if (!member.getRoom().getId().equals(roomId) || member.getStatus() != MemberStatus.ACTIVE) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Участник не найден");
        }
        return build(member, true);
    }

    private RoomMemberEntity requireEditableParticipant(Long userId, Long roomId) {
        RoomMemberEntity member = access.requireActiveMember(roomId, userId);
        if (!member.isParticipates()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Вы не участвуете в раздаче");
        }
        if (!access.canEditOwnContent(member.getRoom(), member)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORM_LOCKED", "Вишлист закрыт. Запросите изменение у админа");
        }
        return member;
    }

    private void apply(WishlistItemEntity item, WishlistDtos.UpsertItemRequest req, Long userId) {
        item.setItemType(req.type());
        item.setTitle(TextSanitizer.requirePlain(req.title(), 200, "Название"));
        item.setBody(TextSanitizer.plain(req.body(), 2000));
        item.setSku(TextSanitizer.plain(req.sku(), 128));
        item.setUrl(null);
        item.setImage(null);

        switch (req.type()) {
            case TEXT -> {
                // body optional
            }
            case LINK -> item.setUrl(TextSanitizer.httpUrl(req.url()));
            case SKU -> {
                if (req.sku() == null || req.sku().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Нужен артикул");
                }
                if (req.url() != null && !req.url().isBlank()) {
                    item.setUrl(TextSanitizer.httpUrl(req.url()));
                }
            }
            case IMAGE -> {
                if (req.imageId() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Нужно изображение");
                }
                ImageEntity image = imageRepository.findById(req.imageId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Картинка не найдена"));
                if (!image.getOwner().getId().equals(userId)) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "IMAGE_FORBIDDEN", "Чужая картинка");
                }
                item.setImage(image);
            }
        }
    }

    private WishlistDtos.WishlistResponse build(RoomMemberEntity member, boolean canEdit) {
        List<WishlistDtos.WishlistItemResponse> items = itemRepository
                .findByMemberIdOrderBySortOrderAscIdAsc(member.getId())
                .stream()
                .map(WishlistService::toDto)
                .toList();
        return new WishlistDtos.WishlistResponse(items, canEdit);
    }

    private static WishlistDtos.WishlistItemResponse toDto(WishlistItemEntity item) {
        ImageEntity image = item.getImage();
        return new WishlistDtos.WishlistItemResponse(
                item.getId(),
                item.getItemType(),
                item.getTitle(),
                item.getBody(),
                item.getUrl(),
                item.getSku(),
                image != null ? image.getId() : null,
                image != null ? image.getDisplayUrl() : null,
                item.getSortOrder()
        );
    }
}
