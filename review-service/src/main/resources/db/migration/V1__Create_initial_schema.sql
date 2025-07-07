
-- Table: entities
CREATE TABLE IF NOT EXISTS entities (
    entity_id INT PRIMARY KEY,
    entity_type VARCHAR NOT NULL, -- e.g., 'hotel', 'airline', 'hostel'
    entity_name VARCHAR NOT NULL
);

-- Table: entity_reviews
CREATE TABLE IF NOT EXISTS entity_reviews (
    review_id BIGINT NOT NULL,
    entity_id INT NOT NULL,
    platform VARCHAR,
    provider_id INT NOT NULL,
    rating DECIMAL(3,1),
    rating_text VARCHAR,
    review_title TEXT,
    review_comments TEXT,
    review_positives TEXT,
    review_negatives TEXT,
    check_in_date VARCHAR,
    review_date TIMESTAMP,
    responder_name VARCHAR,
    response_date VARCHAR,
    response_text TEXT,
    review_provider_text VARCHAR,
    review_provider_logo TEXT,
    encrypted_review_data TEXT,
    original_title TEXT,
    original_comment TEXT,
    PRIMARY KEY (review_id, provider_id),
    CONSTRAINT fk_entity FOREIGN KEY (entity_id) REFERENCES entities (entity_id) ON DELETE CASCADE
);

-- Table: reviewer_info
CREATE TABLE IF NOT EXISTS reviewer_info (
    review_id BIGINT NOT NULL,
    provider_id INT NOT NULL,
    country_id INT,
    country_name VARCHAR,
    flag_name VARCHAR,
    review_group_id INT,
    review_group_name VARCHAR,
    room_type_id INT,
    room_type_name VARCHAR,
    length_of_stay INT,
    reviewer_reviewed_count INT,
    is_expert_reviewer BOOLEAN,
    is_show_global_icon BOOLEAN,
    is_show_reviewed_count BOOLEAN,
    PRIMARY KEY (review_id, provider_id),
    CONSTRAINT fk_review_info FOREIGN KEY (review_id, provider_id)
        REFERENCES entity_reviews (review_id, provider_id)
        ON DELETE CASCADE
);

-- Table: overall_provider_scores
CREATE TABLE IF NOT EXISTS overall_provider_scores (
    entity_id INT NOT NULL,
    provider_id INT NOT NULL,
    review_id BIGINT NOT NULL,
    provider VARCHAR,
    overall_score DECIMAL(3,1),
    review_count INT,
    cleanliness DECIMAL(3,1),
    facilities DECIMAL(3,1),
    location DECIMAL(3,1),
    room_comfort_quality DECIMAL(3,1),
    service DECIMAL(3,1),
    value_for_money DECIMAL(3,1),
    PRIMARY KEY (provider_id, review_id),
    CONSTRAINT fk_entity_score FOREIGN KEY (entity_id) REFERENCES entities (entity_id) ON DELETE CASCADE,
    CONSTRAINT fk_review FOREIGN KEY (review_id, provider_id)
        REFERENCES entity_reviews (review_id, provider_id)
        ON DELETE CASCADE
);

-- Table: review_sources (for managing review data sources)
CREATE TABLE IF NOT EXISTS review_sources (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    uri VARCHAR(255) NOT NULL,    -- e.g. s3://bucket/folder
    credential_json TEXT,         -- encrypted credentials
    active BOOLEAN DEFAULT TRUE,
    last_processed_timestamp TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

-- Table: bad_review_records (for storing invalid review records)
CREATE TABLE IF NOT EXISTS bad_review_records (
    review_id BIGINT NOT NULL,
    provider_id INT NOT NULL,
    json_data JSONB NOT NULL,
    platform VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id, provider_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_bad_review_records_platform ON bad_review_records(platform);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_created_at ON bad_review_records(created_at);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_reason ON bad_review_records(reason);

-- Create GIN index for JSONB queries
CREATE INDEX IF NOT EXISTS idx_bad_review_records_json_data ON bad_review_records USING GIN (json_data);

-- Add comments for documentation
COMMENT ON TABLE bad_review_records IS 'Stores invalid review records that failed validation during processing';
COMMENT ON COLUMN bad_review_records.review_id IS 'Review identifier (part of composite primary key)';
COMMENT ON COLUMN bad_review_records.provider_id IS 'Provider identifier (part of composite primary key)';
COMMENT ON COLUMN bad_review_records.json_data IS 'The original JSON data that failed validation';
COMMENT ON COLUMN bad_review_records.platform IS 'The platform/source where the review came from';
COMMENT ON COLUMN bad_review_records.reason IS 'The reason why the review was considered invalid';
COMMENT ON COLUMN bad_review_records.created_at IS 'Timestamp when the bad record was created'; 