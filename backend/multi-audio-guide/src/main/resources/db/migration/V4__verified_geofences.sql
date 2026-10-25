-- Populated only by the external survey/publication workflow. No demo certifications.
CREATE TABLE geofence_configuration (
    singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton),
    cooldown_seconds INTEGER NOT NULL CHECK (cooldown_seconds BETWEEN 1 AND 2147483)
);

CREATE TABLE location_geofence_surveys (
    location_id UUID PRIMARY KEY REFERENCES locations(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    radius_m INTEGER NOT NULL CHECK (radius_m > 0),
    surveyed_at TIMESTAMPTZ NOT NULL,
    source_reference TEXT NOT NULL CHECK (length(btrim(source_reference)) > 0),
    localized_names JSONB NOT NULL CHECK (jsonb_typeof(localized_names) = 'object'),
    CHECK (jsonb_typeof(localized_names->'VI') = 'string' AND length(btrim(localized_names->>'VI')) > 0
       AND jsonb_typeof(localized_names->'EN') = 'string' AND length(btrim(localized_names->>'EN')) > 0
       AND jsonb_typeof(localized_names->'FR') = 'string' AND length(btrim(localized_names->>'FR')) > 0
       AND jsonb_typeof(localized_names->'JA') = 'string' AND length(btrim(localized_names->>'JA')) > 0
       AND jsonb_typeof(localized_names->'ZH') = 'string' AND length(btrim(localized_names->>'ZH')) > 0
       AND jsonb_typeof(localized_names->'RU') = 'string' AND length(btrim(localized_names->>'RU')) > 0),
    CHECK (localized_names ?& ARRAY['VI', 'EN', 'FR', 'JA', 'ZH', 'RU'])
);
