ALTER TABLE venue_pending_updates
    ADD COLUMN pending_photo_object_key varchar(255);

ALTER TABLE venues DROP CONSTRAINT IF EXISTS venues_status_check;
ALTER TABLE venues
    ADD CONSTRAINT venues_status_check
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED'));