-- PRD section 5.2: Location.status is distinct from GuideScript.status.
-- Existing demo rows are drafts until explicitly approved by the publication dependency.
ALTER TABLE locations ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED'));

ALTER TABLE locations ADD CONSTRAINT ck_approved_location_complete CHECK (
    status <> 'APPROVED' OR (
        name IS NOT NULL AND btrim(name) <> '' AND category_id IS NOT NULL
        AND coordinates IS NOT NULL AND geofence_radius_m IS NOT NULL
    )
);

CREATE INDEX idx_locations_status_category ON locations (status, category_id);
