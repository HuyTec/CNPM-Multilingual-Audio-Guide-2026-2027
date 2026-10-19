-- Demo schema from docs/en/SE.drawio (1).xml, page "Database".
-- The four sections preserve the table groups in that diagram.
-- This is the physical demo model, not the final PRD data model.
-- Location fields and script text remain nullable to support incomplete DRAFTs.
-- No demo records are inserted by this migration.

CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Location catalog (blue).
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE languages (
    code VARCHAR(10) PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES categories(id),
    name VARCHAR(255),
    coordinates GEOGRAPHY(Point, 4326),
    geofence_radius_m INTEGER CHECK (geofence_radius_m > 0)
);

CREATE INDEX idx_locations_coordinates ON locations USING GIST (coordinates);
CREATE INDEX idx_locations_category_id ON locations (category_id);

-- 2. Guide content and optional audio (yellow).
CREATE TABLE guide_content_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL REFERENCES locations(id),
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    script_type VARCHAR(10) NOT NULL CHECK (script_type IN ('FULL', 'SHORT')),
    script_text TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED')),
    revision_no INTEGER NOT NULL DEFAULT 1 CHECK (revision_no > 0),
    CONSTRAINT uq_guide_content_revision
        UNIQUE (location_id, language_code, script_type, revision_no)
);

CREATE TABLE audio_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id UUID NOT NULL UNIQUE REFERENCES guide_content_revisions(id),
    storage_key TEXT NOT NULL,
    format VARCHAR(20) NOT NULL,
    duration_seconds INTEGER CHECK (duration_seconds >= 0),
    CONSTRAINT uq_audio_asset_content UNIQUE (id, content_id)
);

-- 3. Published content packages (green).
CREATE TABLE content_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    scope_type VARCHAR(30) NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE TABLE content_package_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES content_packages(id),
    content_id UUID NOT NULL REFERENCES guide_content_revisions(id),
    audio_id UUID,
    CONSTRAINT uq_content_package_item UNIQUE (package_id, content_id),
    -- Optional audio must belong to the same revision as the package item.
    CONSTRAINT fk_content_package_item_audio
        FOREIGN KEY (audio_id, content_id) REFERENCES audio_assets(id, content_id)
);

-- 4. Feedback and analytics supplied by external integrations (red).
CREATE TABLE feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID REFERENCES locations(id),
    rating SMALLINT,
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW', 'IN_REVIEW', 'RESOLVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID REFERENCES locations(id),
    language_code VARCHAR(10),
    event_type VARCHAR(40) NOT NULL,
    is_offline BOOLEAN NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_events_occurred_at_event_type
    ON analytics_events (occurred_at, event_type);
