-- Add new columns to test_questions table for advanced test features
ALTER TABLE test_questions 
ADD COLUMN IF NOT EXISTS marks INTEGER NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS section_name VARCHAR(255) DEFAULT 'Part-I',
ADD COLUMN IF NOT EXISTS order_index INTEGER DEFAULT 0;

-- Update existing records to have default values
UPDATE test_questions 
SET marks = 1 
WHERE marks IS NULL;

UPDATE test_questions 
SET section_name = 'Part-I' 
WHERE section_name IS NULL;

UPDATE test_questions 
SET order_index = 0 
WHERE order_index IS NULL;
