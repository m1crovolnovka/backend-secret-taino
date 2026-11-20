package com.taino.draw;

import com.taino.common.ApiException;
import com.taino.room.RoomAccessService;
import com.taino.room.RoomEntity;
import com.taino.room.RoomMemberEntity;
import com.taino.room.RoomPhase;
import com.taino.wishlist.WishlistItemEntity;
import com.taino.wishlist.WishlistItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GiftClaimService {

    private final AssignmentRepository assignmentRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final GiftItemClaimRepository claimRepository;
    private final RoomAccessService access;

    public GiftClaimService(
            AssignmentRepository assignmentRepository,
            WishlistItemRepository wishlistItemRepository,
            GiftItemClaimRepository claimRepository,
            RoomAccessService access
    ) {
        this.assignmentRepository = assignmentRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.claimRepository = claimRepository;
        this.access = access;
    }

    @Transactional
    public void claim(Long userId, Long roomId, Long roomItemId) {
        RoomMemberEntity giver = requireGiver(userId, roomId);
        WishlistItemEntity item = requireAssignedItem(giver, roomItemId);
        Long personalId = item.getSourcePersonalItemId();
        if (personalId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NOT_CLAIMABLE",
                    "Эту позицию нельзя отметить между комнатами"
            );
        }

        Optional<GiftItemClaimEntity> existingOpt = claimRepository.findByPersonalItemId(personalId);
        if (existingOpt.isPresent()) {
            GiftItemClaimEntity existing = existingOpt.get();
            if (!existing.getClaimerUserId().equals(userId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "ALREADY_CLAIMED",
                        "Уже отмечено другим дарителем"
                );
            }
            existing.setRoomItemId(item.getId());
            existing.setRoomId(roomId);
            claimRepository.save(existing);
            return;
        }

        GiftItemClaimEntity claim = new GiftItemClaimEntity();
        claim.setPersonalItemId(personalId);
        claim.setRoomItemId(item.getId());
        claim.setClaimerUserId(userId);
        claim.setRoomId(roomId);
        claimRepository.save(claim);
    }

    @Transactional
    public void unclaim(Long userId, Long roomId, Long roomItemId) {
        RoomMemberEntity giver = requireGiver(userId, roomId);
        WishlistItemEntity item = requireAssignedItem(giver, roomItemId);
        Long personalId = item.getSourcePersonalItemId();
        if (personalId == null) {
            return;
        }
        claimRepository.deleteByPersonalItemIdAndClaimerUserId(personalId, userId);
    }

    private RoomMemberEntity requireGiver(Long userId, Long roomId) {
        RoomMemberEntity giver = access.requireActiveMember(roomId, userId);
        RoomEntity room = giver.getRoom();
        if (!giver.isParticipates()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Вы не участвуете в раздаче");
        }
        if (room.getPhase() == RoomPhase.REGISTRATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_DRAWN_YET", "Раздача ещё не прошла");
        }
        assignmentRepository.findByRoomIdAndGiverId(roomId, giver.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_ASSIGNMENT", "Нет назначения"));
        return giver;
    }

    private WishlistItemEntity requireAssignedItem(RoomMemberEntity giver, Long roomItemId) {
        AssignmentEntity assignment = assignmentRepository
                .findByRoomIdAndGiverId(giver.getRoom().getId(), giver.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_ASSIGNMENT", "Нет назначения"));
        WishlistItemEntity item = wishlistItemRepository.findById(roomItemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "Позиция не найдена"));
        if (!item.getMember().getId().equals(assignment.getReceiver().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_YOUR_ASSIGNMENT", "Это не ваш выпавший список");
        }
        return item;
    }
}
