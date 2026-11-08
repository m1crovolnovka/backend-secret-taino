package com.taino.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {
    List<WishlistItemEntity> findByMemberIdOrderBySortOrderAscIdAsc(Long memberId);
    Optional<WishlistItemEntity> findByIdAndMemberId(Long id, Long memberId);
    long countByMemberId(Long memberId);
}
