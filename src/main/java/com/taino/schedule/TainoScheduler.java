package com.taino.schedule;

import com.taino.draw.DrawService;
import com.taino.notification.NotificationService;
import com.taino.room.RoomEntity;
import com.taino.room.RoomPhase;
import com.taino.room.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class TainoScheduler {

    private static final Logger log = LoggerFactory.getLogger(TainoScheduler.class);

    private final RoomRepository roomRepository;
    private final DrawService drawService;
    private final NotificationService notificationService;
    private final int archiveAfterDeliveryDays;

    public TainoScheduler(
            RoomRepository roomRepository,
            DrawService drawService,
            NotificationService notificationService,
            @Value("${taino.scheduler.archive-after-delivery-days:7}") int archiveAfterDeliveryDays
    ) {
        this.roomRepository = roomRepository;
        this.drawService = drawService;
        this.notificationService = notificationService;
        this.archiveAfterDeliveryDays = Math.max(0, archiveAfterDeliveryDays);
    }

    @Scheduled(fixedDelayString = "${taino.scheduler.draw-delay-ms:30000}")
    public void runDraws() {
        Instant now = Instant.now();
        List<RoomEntity> due = roomRepository.findDueForDraw(now);
        for (RoomEntity room : due) {
            try {
                boolean done = drawService.drawRoom(room.getId(), null, false);
                if (done) {
                    log.info("Auto-draw completed for room {}", room.getId());
                }
            } catch (Exception ex) {
                log.warn("Auto-draw failed for room {}: {}", room.getId(), ex.getClass().getSimpleName());
            }
        }
    }

    /** When gift deadline arrives → DELIVERY (room still open for viewing assignment). */
    @Scheduled(fixedDelayString = "${taino.scheduler.delivery-delay-ms:60000}")
    @Transactional
    public void advanceDelivery() {
        for (RoomEntity room : roomRepository.findDueForDelivery(Instant.now())) {
            room.setPhase(RoomPhase.DELIVERY);
            log.info("Room {} entered delivery phase", room.getId());
        }
    }

    /** Days after deliveryAt → FINISHED archive. */
    @Scheduled(fixedDelayString = "${taino.scheduler.archive-delay-ms:120000}")
    @Transactional
    public void archiveFinishedRooms() {
        Instant cutoff = Instant.now().minus(archiveAfterDeliveryDays, ChronoUnit.DAYS);
        for (RoomEntity room : roomRepository.findDueForArchive(cutoff)) {
            room.setPhase(RoomPhase.FINISHED);
            log.info("Room {} archived (FINISHED)", room.getId());
        }
    }

    @Scheduled(fixedDelayString = "${taino.scheduler.notify-delay-ms:20000}")
    public void sendNotifications() {
        try {
            notificationService.processDue();
        } catch (Exception ex) {
            log.warn("Notification dispatch failed: {}", ex.getClass().getSimpleName());
        }
    }
}
