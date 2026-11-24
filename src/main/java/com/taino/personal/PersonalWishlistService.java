package com.taino.personal;

import com.taino.audit.AuditService;
import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import com.taino.config.TainoProperties;
import com.taino.image.ImageEntity;
import com.taino.image.ImageRepository;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomMemberEntity;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import com.taino.wishlist.WishlistItemEntity;
import com.taino.wishlist.WishlistItemRepository;
import com.taino.wishlist.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PersonalWishlistService {

    private final PersonalWishlistRepository wishlistRepository;
    private final PersonalWishlistItemRepository itemRepository;
    private final WishlistItemRepository roomItemRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final RoomAccessService access;
    private final TainoProperties properties;
    private final AuditService auditService;

    public PersonalWishlistService(
            PersonalWishlistRepository wishlistRepository,
            PersonalWishlistItemRepository itemRepository,
            WishlistItemRepository roomItemRepository,
            ImageRepository imageRepository,
            UserRepository userRepository,
            RoomAccessService access,
            TainoProperties properties,
            AuditService auditService
    ) {
        this.wishlistRepository = wishlistRepository;
        this.itemRepository = itemRepository;
        this.roomItemRepository = roomItemRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PersonalWishlistDtos.WishlistSummary> list(Long userId) {
        return wishlistRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(w -> new PersonalWishlistDtos.WishlistSummary(
                        w.getId(),
                        w.getTitle(),
                        w.getShareToken(),
                        telegramShareUrl(w.getShareToken()),
                        webShareUrl(w.getShareToken()),
                        (int) itemRepository.countByWishlistId(w.getId())
                ))
                .toList();
    }

    @Transactional
    public PersonalWishlistDtos.WishlistDetail create(Long userId, PersonalWishlistDtos.CreateWishlistRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Пользователь не найден"));
        PersonalWishlistEntity wishlist = new PersonalWishlistEntity();
        wishlist.setUser(user);
        wishlist.setTitle(TextSanitizer.requirePlain(req.title(), 120, "Название"));
        wishlist.setShareToken(newShareToken());
        wishlist = wishlistRepository.save(wishlist);
        auditService.record(userId, null, "PERSONAL_WISHLIST_CREATED", Map.of("id", wishlist.getId()));
        return toDetail(wishlist);
    }

    @Transactional(readOnly = true)
    public PersonalWishlistDtos.WishlistDetail get(Long userId, Long wishlistId) {
        return toDetail(requireOwned(wishlistId, userId));
    }

    @Transactional
    public PersonalWishlistDtos.WishlistDetail update(Long userId, Long wishlistId, PersonalWishlistDtos.UpdateWishlistRequest req) {
        PersonalWishlistEntity wishlist = requireOwned(wishlistId, userId);
        wishlist.setTitle(TextSanitizer.requirePlain(req.title(), 120, "Название"));
        return toDetail(wishlist);
    }

    @Transactional
    public void delete(Long userId, Long wishlistId) {
        PersonalWishlistEntity wishlist = requireOwned(wishlistId, userId);
        itemRepository.deleteByWishlistId(wishlist.getId());
        wishlistRepository.delete(wishlist);
    }

    @Transactional
    public PersonalWishlistDtos.ItemResponse addItem(Long userId, Long wishlistId, PersonalWishlistDtos.UpsertItemRequest req) {
        PersonalWishlistEntity wishlist = requireOwned(wishlistId, userId);
        if (itemRepository.countByWishlistId(wishlist.getId()) >= WishlistService.MAX_ITEMS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WISHLIST_FULL", "Максимум " + WishlistService.MAX_ITEMS + " пунктов");
        }
        PersonalWishlistItemEntity item = new PersonalWishlistItemEntity();
        item.setWishlist(wishlist);
        apply(item, req, userId);
        item.setSortOrder(req.sortOrder() != null ? req.sortOrder() : (int) itemRepository.countByWishlistId(wishlist.getId()));
        return toItem(itemRepository.save(item));
    }

    @Transactional
    public PersonalWishlistDtos.ItemResponse updateItem(
            Long userId,
            Long wishlistId,
            Long itemId,
            PersonalWishlistDtos.UpsertItemRequest req
    ) {
        requireOwned(wishlistId, userId);
        PersonalWishlistItemEntity item = itemRepository.findByIdAndWishlistId(itemId, wishlistId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "Пункт не найден"));
        apply(item, req, userId);
        if (req.sortOrder() != null) {
            item.setSortOrder(req.sortOrder());
        }
        return toItem(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long userId, Long wishlistId, Long itemId) {
        requireOwned(wishlistId, userId);
        PersonalWishlistItemEntity item = itemRepository.findByIdAndWishlistId(itemId, wishlistId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "Пункт не найден"));
        itemRepository.delete(item);
    }

    @Transactional
    public PersonalWishlistDtos.AttachResponse attachToRoom(Long userId, Long wishlistId, PersonalWishlistDtos.AttachRequest req) {
        PersonalWishlistEntity wishlist = requireOwned(wishlistId, userId);
        RoomMemberEntity member = access.requireActiveMember(req.roomId(), userId);
        if (!member.isParticipates()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Вы не участвуете в раздаче");
        }
        if (!access.canEditOwnContent(member.getRoom(), member)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORM_LOCKED", "Вишлист комнаты закрыт");
        }

        int copied = 0;
        int skipped = 0;
        long current = roomItemRepository.countByMemberId(member.getId());
        java.util.Set<Long> already = roomItemRepository.findByMemberIdOrderBySortOrderAscIdAsc(member.getId())
                .stream()
                .map(WishlistItemEntity::getSourcePersonalItemId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        for (PersonalWishlistItemEntity src : itemRepository.findByWishlistIdOrderBySortOrderAscIdAsc(wishlist.getId())) {
            if (already.contains(src.getId())) {
                skipped++;
                continue;
            }
            if (current >= WishlistService.MAX_ITEMS) {
                skipped++;
                continue;
            }
            WishlistItemEntity item = new WishlistItemEntity();
            item.setMember(member);
            item.setItemType(src.getItemType());
            item.setTitle(src.getTitle());
            item.setBody(src.getBody());
            item.setUrl(src.getUrl());
            item.setSku(src.getSku());
            item.setImage(src.getImage());
            item.setSourcePersonalItemId(src.getId());
            item.setSortOrder((int) current);
            roomItemRepository.save(item);
            already.add(src.getId());
            current++;
            copied++;
        }
        return new PersonalWishlistDtos.AttachResponse(copied, skipped);
    }

    @Transactional(readOnly = true)
    public PersonalWishlistDtos.SharedWishlist getShared(String token) {
        PersonalWishlistEntity wishlist = wishlistRepository.findByShareToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WISHLIST_NOT_FOUND", "Вишлист не найден"));
        List<PersonalWishlistDtos.ItemResponse> items = itemRepository
                .findByWishlistIdOrderBySortOrderAscIdAsc(wishlist.getId())
                .stream()
                .map(PersonalWishlistService::toItem)
                .toList();
        return new PersonalWishlistDtos.SharedWishlist(wishlist.getTitle(), items);
    }

    private PersonalWishlistEntity requireOwned(Long wishlistId, Long userId) {
        return wishlistRepository.findByIdAndUserId(wishlistId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WISHLIST_NOT_FOUND", "Вишлист не найден"));
    }

    private void apply(PersonalWishlistItemEntity item, PersonalWishlistDtos.UpsertItemRequest req, Long userId) {
        item.setItemType(req.type());
        item.setTitle(TextSanitizer.requirePlain(req.title(), 200, "Название"));
        item.setBody(TextSanitizer.plain(req.body(), 2000));
        item.setSku(TextSanitizer.plain(req.sku(), 128));
        item.setUrl(null);
        item.setImage(null);

        switch (req.type()) {
            case TEXT -> {
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

    private String newShareToken() {
        for (int i = 0; i < 8; i++) {
            String token = UUID.randomUUID().toString().replace("-", "");
            if (!wishlistRepository.existsByShareToken(token)) {
                return token;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_GEN", "Не удалось создать ссылку");
    }

    /** Opens Mini App with startapp=wish_TOKEN so recipients stay inside Telegram. */
    private String telegramShareUrl(String token) {
        String payload = "wish_" + token;
        String username = properties.getTelegram().getBotUsername();
        if (username != null && !username.isBlank()) {
            String bot = username.startsWith("@") ? username.substring(1) : username;
            String shortName = properties.getTelegram().getMiniAppShortName();
            if (shortName != null && !shortName.isBlank()) {
                String name = shortName.startsWith("/") ? shortName.substring(1) : shortName;
                return "https://t.me/" + bot + "/" + name + "?startapp=" + payload;
            }
            return "https://t.me/" + bot + "?startapp=" + payload;
        }
        return webShareUrl(token);
    }

    /** Browser preview — no Telegram login required. */
    private String webShareUrl(String token) {
        String mini = properties.getMiniAppUrl();
        if (mini == null || mini.isBlank()) {
            return "/shared/" + token;
        }
        String base = mini.replaceAll("/$", "");
        return base + "/shared/" + token;
    }

    private PersonalWishlistDtos.WishlistDetail toDetail(PersonalWishlistEntity wishlist) {
        List<PersonalWishlistDtos.ItemResponse> items = itemRepository
                .findByWishlistIdOrderBySortOrderAscIdAsc(wishlist.getId())
                .stream()
                .map(PersonalWishlistService::toItem)
                .toList();
        String token = wishlist.getShareToken();
        return new PersonalWishlistDtos.WishlistDetail(
                wishlist.getId(),
                wishlist.getTitle(),
                token,
                telegramShareUrl(token),
                webShareUrl(token),
                items
        );
    }

    private static PersonalWishlistDtos.ItemResponse toItem(PersonalWishlistItemEntity item) {
        ImageEntity image = item.getImage();
        return new PersonalWishlistDtos.ItemResponse(
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
