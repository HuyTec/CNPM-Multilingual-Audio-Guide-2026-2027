CREATE TABLE tourist_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) UNIQUE,
    merged_into UUID REFERENCES tourist_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE tourist_installations (
    key_hash VARCHAR(64) PRIMARY KEY,
    guest_id UUID NOT NULL REFERENCES tourist_users(id)
);
CREATE TABLE tourist_sessions (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tourist_users(id),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE tourist_otp (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    installation_hash VARCHAR(64) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_tourist_otp_created ON tourist_otp(created_at);
ALTER TABLE analytics_events ADD COLUMN listener_id UUID REFERENCES tourist_users(id);
CREATE INDEX idx_analytics_listener ON analytics_events(listener_id);
