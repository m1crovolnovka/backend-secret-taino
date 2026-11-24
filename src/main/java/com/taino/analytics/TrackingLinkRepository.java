package com.taino.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingLinkRepository extends JpaRepository<TrackingLinkEntity, Long> {
    Optional<TrackingLinkEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<TrackingLinkEntity> findAllByOrderByCreatedAtDesc();
}
