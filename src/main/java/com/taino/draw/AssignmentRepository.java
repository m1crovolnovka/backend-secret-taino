package com.taino.draw;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, Long> {
    Optional<AssignmentEntity> findByRoomIdAndGiverId(Long roomId, Long giverMemberId);
    List<AssignmentEntity> findByRoomId(Long roomId);
    void deleteByRoomId(Long roomId);
    boolean existsByRoomId(Long roomId);
}
