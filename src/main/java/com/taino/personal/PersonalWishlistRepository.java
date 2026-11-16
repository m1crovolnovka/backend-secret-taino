package com.taino.personal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalWishlistRepository extends JpaRepository<PersonalWishlistEntity, Long> {
    List<PersonalWishlistEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<PersonalWishlistEntity> findByIdAndUserId(Long id, Long userId);

    Optional<PersonalWishlistEntity> findByShareToken(String shareToken);

    boolean existsByShareToken(String shareToken);
}
