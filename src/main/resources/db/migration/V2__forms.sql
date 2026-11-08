CREATE TABLE forms (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT        NOT NULL REFERENCES room_members (id) ON DELETE CASCADE,
    display_name    VARCHAR(120)  NOT NULL,
    note            VARCHAR(2000),
    clothing_size   VARCHAR(32),
    shoe_size       VARCHAR(32),
    locked          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_forms_member UNIQUE (member_id)
);

CREATE INDEX idx_forms_member ON forms (member_id);
