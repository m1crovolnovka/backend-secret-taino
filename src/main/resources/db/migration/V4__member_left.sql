ALTER TABLE room_members DROP CONSTRAINT IF EXISTS chk_room_members_status;
ALTER TABLE room_members
    ADD CONSTRAINT chk_room_members_status CHECK (status IN ('ACTIVE', 'KICKED', 'LEFT'));
