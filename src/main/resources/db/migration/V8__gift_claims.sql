ALTER TABLE wishlist_items
    ADD COLUMN IF NOT EXISTS source_personal_item_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_wishlist_items_source_personal
    ON wishlist_items (source_personal_item_id);

CREATE TABLE gift_item_claims (
    id              BIGSERIAL PRIMARY KEY,
    personal_item_id BIGINT NOT NULL,
    room_item_id    BIGINT NOT NULL,
    claimer_user_id BIGINT NOT NULL REFERENCES users (id),
    room_id         BIGINT NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    claimed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gift_claim_personal UNIQUE (personal_item_id)
);

CREATE INDEX idx_gift_claims_claimer ON gift_item_claims (claimer_user_id);
CREATE INDEX idx_gift_claims_room ON gift_item_claims (room_id);
