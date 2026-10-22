-- Demo editor workspace. Published location/guide rows remain untouched until external publication.
CREATE TABLE admin_content_drafts (
    location_id UUID PRIMARY KEY,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','PENDING_REVIEW')),
    edit_version INTEGER NOT NULL CHECK (edit_version > 0),
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE admin_audio_uploads (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    script_type VARCHAR(10) NOT NULL CHECK (script_type IN ('FULL','SHORT')),
    filename VARCHAR(255) NOT NULL,
    format VARCHAR(10) NOT NULL CHECK (format IN ('mp3','wav','ogg')),
    data BYTEA NOT NULL CHECK (octet_length(data) BETWEEN 12 AND 20971520),
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
