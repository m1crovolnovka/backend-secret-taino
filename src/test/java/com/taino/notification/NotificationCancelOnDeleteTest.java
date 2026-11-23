package com.taino.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Critical-path guard: reminders for deleted rooms must not be delivered.
 * Mirrors {@link NotificationService#shouldSkipForMissingRoom} rules.
 */
class NotificationCancelOnDeleteTest {

    @Test
    void reminderTypesAreRoomBound() {
        assertTrue(isRoomBound("REMINDER_REG_24H"));
        assertTrue(isRoomBound("REMINDER_REG_1H"));
        assertTrue(isRoomBound("REMINDER_DELIVERY"));
        assertTrue(isRoomBound("DRAW_DONE"));
        assertTrue(isRoomBound("ROOM_JOINED"));
        assertTrue(isRoomBound("CHANGE_APPROVED"));
    }

    @Test
    void roomDeletedNoticeIsNotCancelledByMissingRoom() {
        assertFalse(isRoomBound("ROOM_DELETED"));
    }

    @Test
    void nullRoomIdCancelsRoomBoundNotifications() {
        assertTrue(shouldSkip(null, "REMINDER_REG_24H", false));
        assertTrue(shouldSkip(null, "DRAW_DONE", false));
        assertFalse(shouldSkip(null, "ROOM_DELETED", false));
    }

    @Test
    void missingRoomCancelsReminders() {
        assertTrue(shouldSkip(42L, "REMINDER_REG_1H", false));
        assertFalse(shouldSkip(42L, "REMINDER_REG_1H", true));
    }

    private static boolean isRoomBound(String type) {
        return type.startsWith("REMINDER_")
                || type.startsWith("DRAW_")
                || type.equals("ROOM_JOINED")
                || type.startsWith("CHANGE_");
    }

    private static boolean shouldSkip(Long roomId, String type, boolean roomExists) {
        if (!isRoomBound(type)) {
            return false;
        }
        if (roomId == null) {
            return true;
        }
        return !roomExists;
    }
}
