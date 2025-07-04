-- Remove backend column from review_sources table
ALTER TABLE review_sources DROP COLUMN IF EXISTS backend; 