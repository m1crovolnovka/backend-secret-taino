package com.taino.personal;

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

import java.util.List;

@RestController
@RequestMapping("/api/me/wishlists")
public class PersonalWishlistController {

    private final PersonalWishlistService service;

    public PersonalWishlistController(PersonalWishlistService service) {
        this.service = service;
    }

    @GetMapping
    public List<PersonalWishlistDtos.WishlistSummary> list() {
        return service.list(SecurityUtils.currentUser().userId());
    }

    @PostMapping
    public PersonalWishlistDtos.WishlistDetail create(@Valid @RequestBody PersonalWishlistDtos.CreateWishlistRequest request) {
        return service.create(SecurityUtils.currentUser().userId(), request);
    }

    @GetMapping("/{wishlistId}")
    public PersonalWishlistDtos.WishlistDetail get(@PathVariable Long wishlistId) {
        return service.get(SecurityUtils.currentUser().userId(), wishlistId);
    }

    @PutMapping("/{wishlistId}")
    public PersonalWishlistDtos.WishlistDetail update(
            @PathVariable Long wishlistId,
            @Valid @RequestBody PersonalWishlistDtos.UpdateWishlistRequest request
    ) {
        return service.update(SecurityUtils.currentUser().userId(), wishlistId, request);
    }

    @DeleteMapping("/{wishlistId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long wishlistId) {
        service.delete(SecurityUtils.currentUser().userId(), wishlistId);
    }

    @PostMapping("/{wishlistId}/items")
    public PersonalWishlistDtos.ItemResponse addItem(
            @PathVariable Long wishlistId,
            @Valid @RequestBody PersonalWishlistDtos.UpsertItemRequest request
    ) {
        return service.addItem(SecurityUtils.currentUser().userId(), wishlistId, request);
    }

    @PutMapping("/{wishlistId}/items/{itemId}")
    public PersonalWishlistDtos.ItemResponse updateItem(
            @PathVariable Long wishlistId,
            @PathVariable Long itemId,
            @Valid @RequestBody PersonalWishlistDtos.UpsertItemRequest request
    ) {
        return service.updateItem(SecurityUtils.currentUser().userId(), wishlistId, itemId, request);
    }

    @DeleteMapping("/{wishlistId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long wishlistId, @PathVariable Long itemId) {
        service.deleteItem(SecurityUtils.currentUser().userId(), wishlistId, itemId);
    }

    @PostMapping("/{wishlistId}/attach")
    public PersonalWishlistDtos.AttachResponse attach(
            @PathVariable Long wishlistId,
            @Valid @RequestBody PersonalWishlistDtos.AttachRequest request
    ) {
        return service.attachToRoom(SecurityUtils.currentUser().userId(), wishlistId, request);
    }
}
