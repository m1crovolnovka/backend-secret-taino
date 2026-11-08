package com.taino.room;

public enum AdminMode {
    ADMIN_PARTICIPATES,
    ADMIN_NOT_PARTICIPATES,
    ADMIN_AS_USER;

    public boolean ownerParticipates() {
        return this != ADMIN_NOT_PARTICIPATES;
    }

    /** Admin tab with colors/forms. ADMIN_AS_USER blocks it. */
    public boolean hasAdminTab() {
        return this != ADMIN_AS_USER;
    }
}
