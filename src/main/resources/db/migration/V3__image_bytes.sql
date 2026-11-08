-- Store image binaries in Postgres; URLs become app-relative /api/images/{id}
ALTER TABLE images ADD COLUMN IF NOT EXISTS content BYTEA;

ALTER TABLE images ALTER COLUMN public_url DROP NOT NULL;
ALTER TABLE images ALTER COLUMN display_url DROP NOT NULL;
