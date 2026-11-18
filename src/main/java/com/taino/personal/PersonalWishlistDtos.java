package com.taino.personal;

import com.taino.wishlist.WishlistItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class PersonalWishlistDtos {

    private PersonalWishlistDtos() {
    }

    public record WishlistSummary(
            Long id,
            String title,
            String shareToken,
            /** Telegram Mini App deep-link (preferred for sharing). */
            String telegramShareUrl,
            /** Browser preview URL — works without Telegram login. */
            String webShareUrl,
            int itemCount
    ) {
    }

    public record WishlistDetail(
            Long id,
            String title,
            String shareToken,
            String telegramShareUrl,
            String webShareUrl,
            List<ItemResponse> items
    ) {
    }

    public record ItemResponse(
            Long id,
            WishlistItemType type,
            String title,
            String body,
            String url,
            String sku,
            UUID imageId,
            String imageUrl,
            int sortOrder
    ) {
    }

    public record CreateWishlistRequest(
            @NotBlank @Size(min = 1, max = 120) String title
    ) {
    }

    public record UpdateWishlistRequest(
            @NotBlank @Size(min = 1, max = 120) String title
    ) {
    }

    public record UpsertItemRequest(
            @NotNull WishlistItemType type,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String body,
            @Size(max = 2000) String url,
            @Size(max = 128) String sku,
            UUID imageId,
            Integer sortOrder
    ) {
    }

    public record AttachRequest(@NotNull Long roomId) {
    }

    public record AttachResponse(int copied, int skipped) {
    }

    public record SharedWishlist(
            String title,
            List<ItemResponse> items
    ) {
    }
}
