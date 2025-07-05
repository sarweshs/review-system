ALTER TABLE reviews ADD COLUMN IF NOT EXISTS source VARCHAR(255);
-- Optionally, you can backfill this column here if needed 