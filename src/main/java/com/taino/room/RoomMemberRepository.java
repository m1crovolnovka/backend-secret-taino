package com.taino.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, Long> {

    Optional<RoomMemberEntity> findByRoomIdAndUserId(Long roomId, Long userId);

    List<RoomMemberEntity> findByUserIdAndStatus(Long userId, MemberStatus status);

    List<RoomMemberEntity> findByRoomIdAndStatus(Long roomId, MemberStatus status);

    long countByRoomIdAndStatus(Long roomId, MemberStatus status);

    @Query("select m.colorCode from RoomMemberEntity m where m.room.id = :roomId and m.colorCode is not null")
    Set<String> findUsedColors(@Param("roomId") Long roomId);

    long countByRoomIdAndStatusAndParticipatesTrue(Long roomId, MemberStatus status);
}
