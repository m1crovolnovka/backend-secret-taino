package com.taino.wishlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class WishlistDtos {

    private WishlistDtos() {
    }

    public record WishlistItemResponse(
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

    public record WishlistResponse(List<WishlistItemResponse> items, boolean canEdit) {
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
}
