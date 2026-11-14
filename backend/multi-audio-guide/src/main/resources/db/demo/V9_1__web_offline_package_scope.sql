ALTER TABLE content_package_versions
    DROP CONSTRAINT content_package_versions_scope_check,
    ADD CONSTRAINT content_package_versions_scope_check CHECK (scope IN ('BASE', 'OPTIONAL', 'WEB'));

INSERT INTO content_package_versions(version, job_id, scope, status)
SELECT 'building-' || id || '-WEB', id, 'WEB', 'BUILDING'
FROM publication_jobs
WHERE status = 'BUILDING';
