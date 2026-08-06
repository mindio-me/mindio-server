ALTER TABLE source_clips
    ADD COLUMN was_detected_dead_link BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN manually_confirmed_alive BOOLEAN NOT NULL DEFAULT FALSE;
