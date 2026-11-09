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
@RequestMapping("/api/rooms/{roomId}/me/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistDtos.WishlistResponse get(@PathVariable Long roomId) {
        return wishlistService.getMine(SecurityUtils.currentUser().userId(), roomId);
    }

    @PostMapping("/items")
    public WishlistDtos.WishlistItemResponse add(
            @PathVariable Long roomId,
            @Valid @RequestBody WishlistDtos.UpsertItemRequest request
    ) {
        return wishlistService.addMine(SecurityUtils.currentUser().userId(), roomId, request);
    }

    @PutMapping("/items/{itemId}")
    public WishlistDtos.WishlistItemResponse update(
            @PathVariable Long roomId,
            @PathVariable Long itemId,
            @Valid @RequestBody WishlistDtos.UpsertItemRequest request
    ) {
        return wishlistService.updateMine(SecurityUtils.currentUser().userId(), roomId, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long roomId, @PathVariable Long itemId) {
        wishlistService.deleteMine(SecurityUtils.currentUser().userId(), roomId, itemId);
    }

    @PostMapping("/copy")
    public WishlistDtos.CopyResponse copy(
            @PathVariable Long roomId,
            @Valid @RequestBody WishlistDtos.CopyRequest request
    ) {
        return wishlistService.copyFromRoom(
                SecurityUtils.currentUser().userId(), roomId, request.fromRoomId());
    }
}
