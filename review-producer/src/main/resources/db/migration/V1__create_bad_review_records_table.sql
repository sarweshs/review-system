-- Migration: Create bad_review_records table
-- Description: Table to store invalid review records that fail validation

CREATE TABLE bad_review_records (
    id BIGSERIAL PRIMARY KEY,
    json_data JSONB NOT NULL,
    platform VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_bad_review_records_platform ON bad_review_records(platform);
CREATE INDEX idx_bad_review_records_created_at ON bad_review_records(created_at);
CREATE INDEX idx_bad_review_records_reason ON bad_review_records(reason);

-- Add comments for documentation
COMMENT ON TABLE bad_review_records IS 'Stores invalid review records that fail validation during processing';
COMMENT ON COLUMN bad_review_records.id IS 'Auto-incrementing primary key';
COMMENT ON COLUMN bad_review_records.json_data IS 'The invalid JSON record that failed validation';
COMMENT ON COLUMN bad_review_records.platform IS 'Source platform where the record originated (e.g., aws_s3, minio, gcs)';
COMMENT ON COLUMN bad_review_records.reason IS 'Reason why the record failed validation';
COMMENT ON COLUMN bad_review_records.created_at IS 'Timestamp when the bad record was saved'; 