package com.taino.draw;

import com.taino.form.FormRepository;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomEntity;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomPhase;
import com.taino.wishlist.WishlistItemEntity;
import com.taino.wishlist.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssignmentQueryService {

    private final AssignmentRepository assignmentRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final FormRepository formRepository;
    private final RoomAccessService access;

    public AssignmentQueryService(
            AssignmentRepository assignmentRepository,
            WishlistItemRepository wishlistItemRepository,
            FormRepository formRepository,
            RoomAccessService access
    ) {
        this.assignmentRepository = assignmentRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.formRepository = formRepository;
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
        String note = formRepository.findByMemberId(receiver.getId())
                .map(f -> f.getNote())
                .orElse(null);

        List<AnonymousAssignmentDtos.AnonymousWishlistItem> items = wishlistItemRepository
                .findByMemberIdOrderBySortOrderAscIdAsc(receiver.getId())
                .stream()
                .map(this::toAnonymous)
                .toList();

        return new AnonymousAssignmentDtos.AnonymousAssignmentResponse(
                true,
                null,
                room.getPhase(),
                room.getDeliveryAt(),
                room.getDeliveryPlace(),
                room.getBudgetMin(),
                room.getBudgetMax(),
                room.getCurrency(),
                note,
                items
        );
    }

    private AnonymousAssignmentDtos.AnonymousWishlistItem toAnonymous(WishlistItemEntity item) {
        return new AnonymousAssignmentDtos.AnonymousWishlistItem(
                item.getItemType(),
                item.getTitle(),
                item.getBody(),
                item.getUrl(),
                item.getSku(),
                item.getImage() != null ? item.getImage().getId() : null,
                item.getImage() != null ? item.getImage().getDisplayUrl() : null
        );
    }

    private static AnonymousAssignmentDtos.AnonymousAssignmentResponse unavailable(RoomEntity room, String reason) {
        return new AnonymousAssignmentDtos.AnonymousAssignmentResponse(
                false,
                reason,
                room.getPhase(),
                room.getDeliveryAt(),
                room.getDeliveryPlace(),
                room.getBudgetMin(),
                room.getBudgetMax(),
                room.getCurrency(),
                null,
                List.of()
        );
    }
}
