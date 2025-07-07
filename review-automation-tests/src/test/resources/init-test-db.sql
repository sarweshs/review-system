-- Test Database Initialization Script
-- This script creates the necessary tables for the review system

-- Create entities table
CREATE TABLE IF NOT EXISTS entities (
    entity_id INTEGER PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_name VARCHAR(255) NOT NULL
);

-- Create entity_reviews table with composite primary key
CREATE TABLE IF NOT EXISTS entity_reviews (
    review_id BIGINT NOT NULL,
    provider_id INTEGER NOT NULL,
    entity_id INTEGER NOT NULL,
    platform VARCHAR(50),
    rating NUMERIC(3,1),
    rating_text VARCHAR(255),
    review_title TEXT,
    review_comments TEXT,
    review_positives TEXT,
    review_negatives TEXT,
    check_in_date VARCHAR(100),
    review_date TIMESTAMP,
    responder_name VARCHAR(255),
    response_date VARCHAR(100),
    response_text TEXT,
    review_provider_text VARCHAR(255),
    review_provider_logo VARCHAR(500),
    encrypted_review_data TEXT,
    original_title TEXT,
    original_comment TEXT,
    PRIMARY KEY (review_id, provider_id),
    FOREIGN KEY (entity_id) REFERENCES entities(entity_id)
);

-- Create reviewer_info table with composite primary key
CREATE TABLE IF NOT EXISTS reviewer_info (
    review_id BIGINT NOT NULL,
    provider_id INTEGER NOT NULL,
    country_id INTEGER,
    country_name VARCHAR(100),
    flag_name VARCHAR(100),
    review_group_id INTEGER,
    review_group_name VARCHAR(100),
    room_type_id INTEGER,
    room_type_name VARCHAR(100),
    length_of_stay VARCHAR(50),
    reviewer_reviewed_count INTEGER,
    is_expert_reviewer BOOLEAN,
    is_show_global_icon BOOLEAN,
    is_show_reviewed_count BOOLEAN,
    PRIMARY KEY (review_id, provider_id),
    FOREIGN KEY (review_id, provider_id) REFERENCES entity_reviews(review_id, provider_id)
);

-- Create overall_provider_scores table with composite primary key
CREATE TABLE IF NOT EXISTS overall_provider_scores (
    provider_id INTEGER NOT NULL,
    review_id BIGINT NOT NULL,
    entity_id INTEGER NOT NULL,
    provider VARCHAR(100),
    overall_score NUMERIC(3,1),
    review_count INTEGER,
    cleanliness NUMERIC(3,1),
    facilities NUMERIC(3,1),
    location NUMERIC(3,1),
    room_comfort_quality NUMERIC(3,1),
    service NUMERIC(3,1),
    value_for_money NUMERIC(3,1),
    PRIMARY KEY (provider_id, review_id),
    FOREIGN KEY (review_id, provider_id) REFERENCES entity_reviews(review_id, provider_id),
    FOREIGN KEY (entity_id) REFERENCES entities(entity_id)
);

-- Create bad_review_records table
CREATE TABLE IF NOT EXISTS bad_review_records (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    hotel_name VARCHAR(255),
    platform VARCHAR(50),
    reason VARCHAR(255),
    review_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_entity_reviews_entity_id ON entity_reviews(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_reviews_platform ON entity_reviews(platform);
CREATE INDEX IF NOT EXISTS idx_entity_reviews_rating ON entity_reviews(rating);
CREATE INDEX IF NOT EXISTS idx_entity_reviews_review_date ON entity_reviews(review_date);
CREATE INDEX IF NOT EXISTS idx_overall_provider_scores_entity_id ON overall_provider_scores(entity_id);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_hotel_id ON bad_review_records(hotel_id);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_platform ON bad_review_records(platform);
CREATE INDEX IF NOT EXISTS idx_bad_review_records_created_at ON bad_review_records(created_at);

-- Insert some test data
INSERT INTO entities (entity_id, entity_type, entity_name) VALUES 
(1, 'HOTEL', 'Test Hotel 1'),
(2, 'HOTEL', 'Test Hotel 2'),
(3, 'HOTEL', 'Test Hotel 3')
ON CONFLICT (entity_id) DO NOTHING; 