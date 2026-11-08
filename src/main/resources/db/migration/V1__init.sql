CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    telegram_id     BIGINT      NOT NULL,
    username        VARCHAR(64),
    first_name      VARCHAR(128) NOT NULL,
    last_name       VARCHAR(128),
    language_code   VARCHAR(16),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_telegram_id UNIQUE (telegram_id)
);

CREATE INDEX idx_users_username ON users (username);

CREATE TABLE rooms (
    id                      BIGSERIAL PRIMARY KEY,
    code                    VARCHAR(16)  NOT NULL,
    title                   VARCHAR(120) NOT NULL,
    theme                   VARCHAR(32)  NOT NULL DEFAULT 'newyear',
    phase                   VARCHAR(32)  NOT NULL DEFAULT 'REGISTRATION',
    admin_mode              VARCHAR(32)  NOT NULL,
    owner_id                BIGINT       NOT NULL REFERENCES users (id),
    registration_ends_at    TIMESTAMPTZ  NOT NULL,
    delivery_at             TIMESTAMPTZ  NOT NULL,
    delivery_place          VARCHAR(500),
    budget_min              INTEGER      NOT NULL DEFAULT 0,
    budget_max              INTEGER      NOT NULL DEFAULT 0,
    currency                VARCHAR(8)   NOT NULL DEFAULT 'BYN',
    draw_locked             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rooms_code UNIQUE (code),
    CONSTRAINT chk_rooms_phase CHECK (phase IN ('REGISTRATION', 'DRAW_DONE', 'DELIVERY', 'FINISHED')),
    CONSTRAINT chk_rooms_admin_mode CHECK (admin_mode IN ('ADMIN_PARTICIPATES', 'ADMIN_NOT_PARTICIPATES', 'ADMIN_AS_USER')),
    CONSTRAINT chk_rooms_budget CHECK (budget_min >= 0 AND budget_max >= budget_min),
    CONSTRAINT chk_rooms_dates CHECK (delivery_at > registration_ends_at)
);

CREATE INDEX idx_rooms_phase_registration ON rooms (phase, registration_ends_at);
CREATE INDEX idx_rooms_owner ON rooms (owner_id);

CREATE TABLE room_members (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT       NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    role            VARCHAR(16)  NOT NULL DEFAULT 'MEMBER',
    participates    BOOLEAN      NOT NULL DEFAULT TRUE,
    color_code      VARCHAR(32),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    edit_window_until TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_members_room_user UNIQUE (room_id, user_id),
    CONSTRAINT uq_room_members_room_color UNIQUE (room_id, color_code),
    CONSTRAINT chk_room_members_role CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT chk_room_members_status CHECK (status IN ('ACTIVE', 'KICKED'))
);

CREATE INDEX idx_room_members_user ON room_members (user_id);
CREATE INDEX idx_room_members_room_status ON room_members (room_id, status);

CREATE TABLE images (
    id              UUID PRIMARY KEY,
    owner_user_id   BIGINT        NOT NULL REFERENCES users (id),
    storage_key     VARCHAR(128)  NOT NULL,
    public_url      VARCHAR(1000) NOT NULL,
    display_url     VARCHAR(1000) NOT NULL,
    delete_url      VARCHAR(1000),
    thumb_url       VARCHAR(1000),
    mime_type       VARCHAR(64)   NOT NULL,
    width           INTEGER,
    height          INTEGER,
    byte_size       INTEGER       NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_images_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_images_owner ON images (owner_user_id);

CREATE TABLE wishlist_items (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT       NOT NULL REFERENCES room_members (id) ON DELETE CASCADE,
    item_type       VARCHAR(16)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(2000),
    url             VARCHAR(2000),
    sku             VARCHAR(128),
    image_id        UUID REFERENCES images (id) ON DELETE SET NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_wishlist_item_type CHECK (item_type IN ('TEXT', 'LINK', 'SKU', 'IMAGE'))
);

CREATE INDEX idx_wishlist_items_member ON wishlist_items (member_id, sort_order);

CREATE TABLE change_requests (
    id                  BIGSERIAL PRIMARY KEY,
    room_id             BIGINT       NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    member_id           BIGINT       NOT NULL REFERENCES room_members (id) ON DELETE CASCADE,
    reason              VARCHAR(1000) NOT NULL,
    requested_changes   VARCHAR(2000) NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    admin_note          VARCHAR(1000),
    resolved_by_user_id BIGINT REFERENCES users (id),
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_change_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_change_requests_room_status ON change_requests (room_id, status);

CREATE TABLE assignments (
    id                  BIGSERIAL PRIMARY KEY,
    room_id             BIGINT       NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    giver_member_id     BIGINT       NOT NULL REFERENCES room_members (id) ON DELETE CASCADE,
    receiver_member_id  BIGINT       NOT NULL REFERENCES room_members (id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_assignments_room_giver UNIQUE (room_id, giver_member_id),
    CONSTRAINT uq_assignments_room_receiver UNIQUE (room_id, receiver_member_id),
    CONSTRAINT chk_assignments_not_self CHECK (giver_member_id <> receiver_member_id)
);

CREATE INDEX idx_assignments_room ON assignments (room_id);

CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    room_id         BIGINT REFERENCES rooms (id) ON DELETE SET NULL,
    type            VARCHAR(64)  NOT NULL,
    payload_json    TEXT,
    scheduled_at    TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_notifications_due ON notifications (status, scheduled_at);

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   BIGINT REFERENCES users (id),
    room_id         BIGINT REFERENCES rooms (id) ON DELETE SET NULL,
    action          VARCHAR(64)  NOT NULL,
    meta_json       TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_room ON audit_log (room_id, created_at DESC);
