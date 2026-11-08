package com.taino.draw;

import com.taino.room.RoomPhase;
import com.taino.wishlist.WishlistItemType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AnonymousAssignmentDtos {

    private AnonymousAssignmentDtos() {
    }

    public record AnonymousAssignmentResponse(
            boolean available,
            String reason,
            RoomPhase phase,
            Instant deliveryAt,
            String deliveryPlace,
            int budgetMin,
            int budgetMax,
            String currency,
            String note,
            List<AnonymousWishlistItem> wishlist
    ) {
    }

    /**
     * No owner userId, username, color, or original filenames.
     */
    public record AnonymousWishlistItem(
            WishlistItemType type,
            String title,
            String body,
            String url,
            String sku,
            UUID imageId,
            String imageUrl
    ) {
    }
}
