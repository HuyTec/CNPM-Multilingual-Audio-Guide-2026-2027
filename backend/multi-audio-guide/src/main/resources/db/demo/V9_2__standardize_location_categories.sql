-- Keep the category IDs used by saved URLs and the offline package scopes.
UPDATE categories SET name = 'Thường', is_active = TRUE
WHERE id = '10000000-0000-0000-0000-000000000101';

UPDATE categories SET name = 'Công viên', is_active = TRUE
WHERE id = '10000000-0000-0000-0000-000000000003';

-- Keep public locations in any additional active category discoverable.
UPDATE locations SET category_id = '10000000-0000-0000-0000-000000000101'
WHERE category_id IN (
    SELECT id FROM categories
    WHERE is_active AND name NOT IN ('Thường', 'Di tích', 'Bảo tàng', 'Công viên')
);

UPDATE categories
SET is_active = name IN ('Thường', 'Di tích', 'Bảo tàng', 'Công viên')
WHERE is_active IS DISTINCT FROM (name IN ('Thường', 'Di tích', 'Bảo tàng', 'Công viên'));

UPDATE admin_content_drafts
SET payload = jsonb_set(payload, '{category}', to_jsonb(
    CASE WHEN payload->>'category' = 'Công viên demo (trống)' THEN 'Công viên' ELSE 'Thường' END
))
WHERE btrim(payload->>'category') <> ''
  AND payload->>'category' NOT IN ('Thường', 'Di tích', 'Bảo tàng', 'Công viên');

-- Only pending and retryable snapshots can still publish.
UPDATE publication_jobs
SET payload = jsonb_set(payload, '{category}', to_jsonb(
    CASE WHEN payload->>'category' = 'Công viên demo (trống)' THEN 'Công viên' ELSE 'Thường' END
))
WHERE status IN ('BUILDING', 'FAILED') AND btrim(payload->>'category') <> ''
  AND payload->>'category' NOT IN ('Thường', 'Di tích', 'Bảo tàng', 'Công viên');

-- Rebuild signed packages rather than modifying previously signed payloads.
WITH job AS (
    INSERT INTO publication_jobs(id, status)
    SELECT gen_random_uuid(), 'BUILDING'
    WHERE EXISTS (SELECT 1 FROM content_package_versions WHERE scope = 'WEB')
      AND NOT EXISTS (SELECT 1 FROM publication_jobs WHERE status = 'BUILDING')
    RETURNING id
)
INSERT INTO content_package_versions(version, job_id, scope, status)
SELECT 'building-' || job.id || '-' || scopes.scope, job.id, scopes.scope, 'BUILDING'
FROM job CROSS JOIN (VALUES ('BASE'), ('OPTIONAL'), ('WEB')) AS scopes(scope);
