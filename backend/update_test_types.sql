-- Set default test type for existing tests
UPDATE tests SET type = 'HYBRID' WHERE type IS NULL;
