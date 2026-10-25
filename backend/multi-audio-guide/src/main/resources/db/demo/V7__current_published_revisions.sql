-- Preserve historical content IDs for offline listening receipts while selecting the current publication.
ALTER TABLE guide_content_revisions ADD COLUMN is_current BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX current_guide_variants ON guide_content_revisions(location_id,language_code,script_type,revision_no DESC) WHERE is_current;
