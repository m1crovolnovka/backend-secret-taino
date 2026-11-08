package com.taino.wishlist;

import com.taino.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
