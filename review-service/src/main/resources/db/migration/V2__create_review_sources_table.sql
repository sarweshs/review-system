CREATE TABLE IF NOT EXISTS review_sources (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    backend VARCHAR(20) NOT NULL, -- e.g. s3, minio, gcs
    uri VARCHAR(255) NOT NULL,    -- e.g. s3://bucket/folder
    last_processed_timestamp TIMESTAMP DEFAULT NULL
); 