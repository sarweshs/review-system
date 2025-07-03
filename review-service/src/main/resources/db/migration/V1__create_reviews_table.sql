CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL PRIMARY KEY,
    hotel_id BIGINT,
    platform VARCHAR(32),
    hotel_name VARCHAR(255),
    comment JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Index for faster lookups by hotel_id/platform
CREATE INDEX IF NOT EXISTS idx_reviews_hotel_platform ON reviews(hotel_id, platform); 