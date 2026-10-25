CREATE TABLE visitor_installations (
    installation_id UUID NOT NULL,
    platform VARCHAR(10) NOT NULL CHECK (platform IN ('WEB', 'ANDROID')),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (installation_id, platform)
);
CREATE INDEX visitor_recent_activity ON visitor_installations (last_seen_at);
ALTER TABLE analytics_events ADD COLUMN installation_id UUID;
ALTER TABLE analytics_events ADD COLUMN client_platform VARCHAR(10) CHECK (client_platform IN ('WEB', 'ANDROID'));
