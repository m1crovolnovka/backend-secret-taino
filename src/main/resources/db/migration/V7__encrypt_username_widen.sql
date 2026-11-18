-- Encrypted PII fields are longer than plaintext (ENC1: + base64 AES-GCM).
ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(512);
ALTER TABLE users ALTER COLUMN first_name TYPE VARCHAR(512);
ALTER TABLE users ALTER COLUMN last_name TYPE VARCHAR(512);
