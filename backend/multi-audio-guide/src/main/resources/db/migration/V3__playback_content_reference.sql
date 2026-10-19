-- Event UUID is supplied by the client and is the idempotency key for retries.
ALTER TABLE analytics_events ADD COLUMN content_id UUID REFERENCES guide_content_revisions(id);
CREATE INDEX idx_analytics_events_content_id ON analytics_events(content_id);
