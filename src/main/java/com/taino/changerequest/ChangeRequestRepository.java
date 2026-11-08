package com.taino.changerequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequestEntity, Long> {
    List<ChangeRequestEntity> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    Optional<ChangeRequestEntity> findByIdAndRoomId(Long id, Long roomId);
    boolean existsByMemberIdAndStatus(Long memberId, String status);
}
