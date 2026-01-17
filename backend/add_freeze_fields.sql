-- Add freeze tracking fields to student_attempts table
-- This supports auto-freeze functionality for critical violations

ALTER TABLE student_attempts 
ADD COLUMN IF NOT EXISTS freeze_reason VARCHAR(500),
ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMP;

-- Create index for faster queries on frozen attempts
CREATE INDEX IF NOT EXISTS idx_student_attempts_frozen 
ON student_attempts(frozen_at) 
WHERE frozen_at IS NOT NULL;

-- Verify the changes
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'student_attempts' 
AND column_name IN ('freeze_reason', 'frozen_at');
