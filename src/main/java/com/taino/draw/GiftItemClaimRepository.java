package com.taino.draw;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GiftItemClaimRepository extends JpaRepository<GiftItemClaimEntity, Long> {

    Optional<GiftItemClaimEntity> findByPersonalItemId(Long personalItemId);

    List<GiftItemClaimEntity> findByPersonalItemIdIn(Collection<Long> personalItemIds);

    void deleteByPersonalItemIdAndClaimerUserId(Long personalItemId, Long claimerUserId);
}
