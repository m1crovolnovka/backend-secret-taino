CREATE TABLE staff_accounts (
    id                      BIGSERIAL PRIMARY KEY,
    login                   VARCHAR(64)  NOT NULL,
    password_hash           VARCHAR(100) NOT NULL,
    must_change_password    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_staff_accounts_login UNIQUE (login)
);

CREATE TABLE tracking_links (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32)  NOT NULL,
    title           VARCHAR(160) NOT NULL,
    utm_source      VARCHAR(120),
    utm_medium      VARCHAR(120),
    utm_campaign    VARCHAR(160),
    utm_content     VARCHAR(160),
    utm_term        VARCHAR(160),
    label           VARCHAR(160),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by_id   BIGINT       REFERENCES staff_accounts (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tracking_links_code UNIQUE (code)
);

CREATE INDEX idx_tracking_links_active ON tracking_links (active);

CREATE TABLE analytics_events (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(48)  NOT NULL,
    link_id         BIGINT       REFERENCES tracking_links (id) ON DELETE SET NULL,
    user_id         BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    visitor_key     VARCHAR(64),
    referrer        VARCHAR(1000),
    user_agent      VARCHAR(500),
    ip_hash         VARCHAR(64),
    meta_json       TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_events_type_created ON analytics_events (event_type, created_at);
CREATE INDEX idx_analytics_events_link_created ON analytics_events (link_id, created_at);
CREATE INDEX idx_analytics_events_user_created ON analytics_events (user_id, created_at);
CREATE INDEX idx_analytics_events_visitor ON analytics_events (visitor_key);

ALTER TABLE users
    ADD COLUMN attribution_link_id BIGINT REFERENCES tracking_links (id) ON DELETE SET NULL;

CREATE INDEX idx_users_attribution_link ON users (attribution_link_id);
