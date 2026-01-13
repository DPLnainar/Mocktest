ALTER TABLE questions DROP COLUMN IF EXISTS language_id;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS allowed_language_ids JSONB;
