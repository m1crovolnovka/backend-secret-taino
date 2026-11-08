package com.taino.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
            select n from NotificationEntity n
            where n.status = 'PENDING' and n.scheduledAt <= :now
            order by n.scheduledAt asc
            """)
    List<NotificationEntity> findDue(@Param("now") Instant now);

    boolean existsByRoomIdAndUserIdAndTypeAndStatus(Long roomId, Long userId, String type, String status);
}
