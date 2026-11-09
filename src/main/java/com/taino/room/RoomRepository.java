package com.taino.room;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    Optional<RoomEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomEntity r where r.id = :id")
    Optional<RoomEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select r from RoomEntity r
            where r.phase = com.taino.room.RoomPhase.REGISTRATION
              and r.drawLocked = false
              and r.registrationEndsAt <= :now
            """)
    List<RoomEntity> findDueForDraw(@Param("now") Instant now);

    @Query("""
            select r from RoomEntity r
            where r.phase = com.taino.room.RoomPhase.DRAW_DONE
              and r.deliveryAt <= :now
            """)
    List<RoomEntity> findDueForDelivery(@Param("now") Instant now);

    /** Rooms past delivery that should move into the archive (FINISHED). */
    @Query("""
            select r from RoomEntity r
            where r.phase = com.taino.room.RoomPhase.DELIVERY
              and r.deliveryAt <= :cutoff
            """)
    List<RoomEntity> findDueForArchive(@Param("cutoff") Instant cutoff);
}
