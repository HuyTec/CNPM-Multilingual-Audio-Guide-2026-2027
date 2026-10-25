-- Published translations are changed only by the external publication dependency.
CREATE TABLE location_translations (
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (location_id, language_code)
);
INSERT INTO location_translations(location_id,language_code,name)
SELECT l.id,'VI',l.name FROM locations l WHERE l.name IS NOT NULL
    AND EXISTS (SELECT 1 FROM languages WHERE code='VI');

-- Feedback already exists in V1; evolve it without discarding external records.
ALTER TABLE feedbacks ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT 'Khác';
ALTER TABLE feedbacks ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE feedbacks ADD COLUMN language_code VARCHAR(10);
ALTER TABLE feedbacks ADD CONSTRAINT ck_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5) NOT VALID;
CREATE INDEX idx_feedback_period_status_category ON feedbacks(created_at DESC,status,category,id);
