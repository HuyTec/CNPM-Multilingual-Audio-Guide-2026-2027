ALTER TABLE admin_content_drafts DROP CONSTRAINT admin_content_drafts_status_check;
ALTER TABLE admin_content_drafts ADD CONSTRAINT admin_content_drafts_status_check CHECK(status IN ('DRAFT','PENDING_REVIEW','APPROVED','REJECTED','PUBLISHED'));
CREATE TABLE admin_accounts (username VARCHAR(100) PRIMARY KEY, password_hash TEXT NOT NULL, role VARCHAR(20) NOT NULL CHECK(role IN ('EDITOR','REVIEWER')), active BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE admin_sessions (token_hash VARCHAR(64) PRIMARY KEY, username VARCHAR(100) NOT NULL REFERENCES admin_accounts, expires_at TIMESTAMPTZ NOT NULL);
CREATE TABLE package_signing_keys (singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK(singleton), private_key BYTEA NOT NULL, public_key BYTEA NOT NULL);
CREATE TABLE publication_jobs (id UUID PRIMARY KEY, location_id UUID, edit_version INTEGER, payload JSONB, status VARCHAR(20) NOT NULL CHECK(status IN ('BUILDING','READY','FAILED')), error_log TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMPTZ);
CREATE UNIQUE INDEX one_active_publication ON publication_jobs(location_id) WHERE status='BUILDING';
CREATE TABLE content_package_versions (version TEXT PRIMARY KEY, job_id UUID NOT NULL REFERENCES publication_jobs, scope VARCHAR(20) NOT NULL CHECK(scope IN ('BASE','OPTIONAL')), status VARCHAR(20) NOT NULL CHECK(status IN ('BUILDING','READY','FAILED')), checksum VARCHAR(64), signature TEXT, size_bytes BIGINT, manifest JSONB, payload BYTEA, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, ready_at TIMESTAMPTZ, error_log TEXT);
CREATE INDEX package_latest_ready ON content_package_versions(scope,ready_at DESC) WHERE status='READY';
