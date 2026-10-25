ALTER TABLE admin_content_drafts DROP CONSTRAINT admin_content_drafts_status_check;
ALTER TABLE admin_content_drafts ADD CONSTRAINT admin_content_drafts_status_check
    CHECK (status IN ('DRAFT','PENDING_REVIEW','REJECTED'));
ALTER TABLE admin_content_drafts ADD COLUMN reviewer_comment TEXT;
UPDATE admin_content_drafts SET payload=jsonb_set(payload,'{names}',jsonb_build_object('vi',COALESCE(payload->>'name','')))
WHERE NOT (payload ? 'names');
