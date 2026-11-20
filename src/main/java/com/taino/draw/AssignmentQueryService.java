package com.taino.draw;

import com.taino.form.FormEntity;
import com.taino.form.FormRepository;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomEntity;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomPhase;
import com.taino.room.SecrecyMode;
import com.taino.wishlist.WishlistItemEntity;
import com.taino.wishlist.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssignmentQueryService {

    private final AssignmentRepository assignmentRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final FormRepository formRepository;
    private final GiftItemClaimRepository claimRepository;
    private final RoomAccessService access;

    public AssignmentQueryService(
            AssignmentRepository assignmentRepository,
            WishlistItemRepository wishlistItemRepository,
            FormRepository formRepository,
            GiftItemClaimRepository claimRepository,
            RoomAccessService access
    ) {
        this.assignmentRepository = assignmentRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.formRepository = formRepository;
        this.claimRepository = claimRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public AnonymousAssignmentDtos.AnonymousAssignmentResponse getMine(Long userId, Long roomId) {
        RoomMemberEntity giver = access.requireActiveMember(roomId, userId);
        RoomEntity room = giver.getRoom();

        if (!giver.isParticipates()) {
            return unavailable(room, "NOT_PARTICIPANT");
        }
        if (room.getPhase() == RoomPhase.REGISTRATION) {
            return unavailable(room, "NOT_DRAWN_YET");
        }

        AssignmentEntity assignment = assignmentRepository.findByRoomIdAndGiverId(roomId, giver.getId())
                .orElse(null);
        if (assignment == null) {
            return unavailable(room, "NO_ASSIGNMENT");
        }

        RoomMemberEntity receiver = assignment.getReceiver();
        FormEntity form = formRepository.findByMemberId(receiver.getId()).orElse(null);
        boolean open = room.getSecrecyMode() == SecrecyMode.OPEN;

        List<WishlistItemEntity> rawItems = wishlistItemRepository
                .findByMemberIdOrderBySortOrderAscIdAsc(receiver.getId());

        List<Long> personalIds = rawItems.stream()
                .map(WishlistItemEntity::getSourcePersonalItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, GiftItemClaimEntity> claimsByPersonal = personalIds.isEmpty()
                ? Map.of()
                : claimRepository.findByPersonalItemIdIn(personalIds).stream()
                        .collect(Collectors.toMap(GiftItemClaimEntity::getPersonalItemId, Function.identity()));

        List<AnonymousAssignmentDtos.AnonymousWishlistItem> items = rawItems.stream()
                .map(item -> toAnonymous(item, userId, claimsByPersonal))
                .toList();

        return new AnonymousAssignmentDtos.AnonymousAssignmentResponse(
                true,
                null,
                room.getPhase(),
                room.getSecrecyMode() != null ? room.getSecrecyMode() : SecrecyMode.SECRET,
                room.getDeliveryAt(),
                room.getDeliveryPlace(),
                room.getBudgetMin(),
                room.getBudgetMax(),
                room.getCurrency(),
                receiver.getColorCode(),
                form != null ? form.getNote() : null,
                open && form != null ? form.getWbPickup() : null,
                open && form != null ? form.getTelegramLink() : null,
                items
        );
    }

    private AnonymousAssignmentDtos.AnonymousWishlistItem toAnonymous(
            WishlistItemEntity item,
            Long viewerUserId,
            Map<Long, GiftItemClaimEntity> claimsByPersonal
    ) {
        Long personalId = item.getSourcePersonalItemId();
        boolean claimable = personalId != null;
        boolean claimedByMe = false;
        boolean claimedByOther = false;
        if (claimable) {
            GiftItemClaimEntity claim = claimsByPersonal.get(personalId);
            if (claim != null) {
                if (claim.getClaimerUserId().equals(viewerUserId)) {
                    claimedByMe = true;
                } else {
                    claimedByOther = true;
                }
            }
        }
        return new AnonymousAssignmentDtos.AnonymousWishlistItem(
                item.getId(),
                item.getItemType(),
                item.getTitle(),
                item.getBody(),
                item.getUrl(),
                item.getSku(),
                item.getImage() != null ? item.getImage().getId() : null,
                item.getImage() != null ? item.getImage().getDisplayUrl() : null,
                claimable,
                claimedByMe,
                claimedByOther
        );
    }

    private static AnonymousAssignmentDtos.AnonymousAssignmentResponse unavailable(RoomEntity room, String reason) {
        return new AnonymousAssignmentDtos.AnonymousAssignmentResponse(
                false,
                reason,
                room.getPhase(),
                room.getSecrecyMode() != null ? room.getSecrecyMode() : SecrecyMode.SECRET,
                room.getDeliveryAt(),
                room.getDeliveryPlace(),
                room.getBudgetMin(),
                room.getBudgetMax(),
                room.getCurrency(),
                null,
                null,
                null,
                null,
                List.of()
        );
    }
}
