package com.taino.personal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlists/shared")
public class SharedWishlistController {

    private final PersonalWishlistService service;

    public SharedWishlistController(PersonalWishlistService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public PersonalWishlistDtos.SharedWishlist get(@PathVariable String token) {
        return service.getShared(token);
    }
}
