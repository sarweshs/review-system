-- Create bad_review_records table for storing invalid review records
CREATE TABLE IF NOT EXISTS bad_review_records (
    id BIGSERIAL PRIMARY KEY,
    json_data JSONB NOT NULL,
    platform VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_bad_review_records_platform ON bad_review_records(platform);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_created_at ON bad_review_records(created_at);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_reason ON bad_review_records(reason);

-- Create GIN index for JSONB queries
CREATE INDEX IF NOT EXISTS idx_bad_review_records_json_data ON bad_review_records USING GIN (json_data);

-- Add comments for documentation
COMMENT ON TABLE bad_review_records IS 'Stores invalid review records that failed validation during processing';
COMMENT ON COLUMN bad_review_records.id IS 'Primary key';
COMMENT ON COLUMN bad_review_records.json_data IS 'The original JSON data that failed validation';
COMMENT ON COLUMN bad_review_records.platform IS 'The platform/source where the review came from';
COMMENT ON COLUMN bad_review_records.reason IS 'The reason why the review was considered invalid';
COMMENT ON COLUMN bad_review_records.created_at IS 'Timestamp when the bad record was created'; 