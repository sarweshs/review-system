CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL PRIMARY KEY,
    provider VARCHAR(32),
    review_id VARCHAR(128) UNIQUE,
    hotel_id VARCHAR(128),
    user_id VARCHAR(128),
    content TEXT,
    rating INT,
    review_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
); 