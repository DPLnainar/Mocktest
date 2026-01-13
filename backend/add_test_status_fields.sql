-- Add new columns to tests table for enhanced test builder
ALTER TABLE tests 
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
ADD COLUMN IF NOT EXISTS test_type VARCHAR(100),
ADD COLUMN IF NOT EXISTS instructions TEXT;

-- Update existing tests to have DRAFT status
UPDATE tests 
SET status = 'DRAFT' 
WHERE status IS NULL;

-- Add check constraint for status
ALTER TABLE tests
ADD CONSTRAINT check_test_status 
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
