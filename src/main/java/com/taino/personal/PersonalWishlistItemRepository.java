package com.taino.personal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalWishlistItemRepository extends JpaRepository<PersonalWishlistItemEntity, Long> {
    List<PersonalWishlistItemEntity> findByWishlistIdOrderBySortOrderAscIdAsc(Long wishlistId);

    long countByWishlistId(Long wishlistId);

    Optional<PersonalWishlistItemEntity> findByIdAndWishlistId(Long id, Long wishlistId);

    void deleteByWishlistId(Long wishlistId);
}
