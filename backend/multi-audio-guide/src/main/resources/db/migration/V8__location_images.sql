CREATE TABLE location_image_uploads (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL,
    data BYTEA NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE locations ADD COLUMN image_url TEXT;
