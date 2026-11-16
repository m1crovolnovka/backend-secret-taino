ALTER TABLE rooms
    ADD COLUMN secrecy_mode VARCHAR(16) NOT NULL DEFAULT 'SECRET',
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE rooms
    ADD CONSTRAINT chk_rooms_secrecy_mode CHECK (secrecy_mode IN ('SECRET', 'OPEN')),
    ADD CONSTRAINT chk_rooms_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC'));

CREATE INDEX idx_rooms_public_search ON rooms (visibility, phase, title);

ALTER TABLE forms
    ADD COLUMN wb_pickup VARCHAR(500),
    ADD COLUMN telegram_link VARCHAR(500);

CREATE TABLE personal_wishlists (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           VARCHAR(120) NOT NULL,
    share_token     VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_personal_wishlists_share UNIQUE (share_token)
);

CREATE INDEX idx_personal_wishlists_user ON personal_wishlists (user_id);

CREATE TABLE personal_wishlist_items (
    id              BIGSERIAL PRIMARY KEY,
    wishlist_id     BIGINT       NOT NULL REFERENCES personal_wishlists (id) ON DELETE CASCADE,
    item_type       VARCHAR(16)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(2000),
    url             VARCHAR(2000),
    sku             VARCHAR(128),
    image_id        UUID REFERENCES images (id) ON DELETE SET NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_personal_wishlist_item_type CHECK (item_type IN ('TEXT', 'LINK', 'SKU', 'IMAGE'))
);

CREATE INDEX idx_personal_wishlist_items_list ON personal_wishlist_items (wishlist_id, sort_order);
