package com.taino.wishlist;

import com.taino.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}/members/{memberId}/wishlist")
public class AdminWishlistController {

    private final WishlistService wishlistService;

    public AdminWishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistDtos.WishlistResponse get(@PathVariable Long roomId, @PathVariable Long memberId) {
        return wishlistService.adminGet(SecurityUtils.currentUser().userId(), roomId, memberId);
    }

    @PostMapping("/items")
    public WishlistDtos.WishlistItemResponse add(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @Valid @RequestBody WishlistDtos.UpsertItemRequest request
    ) {
        return wishlistService.adminAdd(SecurityUtils.currentUser().userId(), roomId, memberId, request);
    }

    @PutMapping("/items/{itemId}")
    public WishlistDtos.WishlistItemResponse update(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @PathVariable Long itemId,
            @Valid @RequestBody WishlistDtos.UpsertItemRequest request
    ) {
        return wishlistService.adminUpdate(SecurityUtils.currentUser().userId(), roomId, memberId, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @PathVariable Long itemId
    ) {
        wishlistService.adminDelete(SecurityUtils.currentUser().userId(), roomId, memberId, itemId);
    }
}
