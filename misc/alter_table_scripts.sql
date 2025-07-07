ALTER TABLE entity_reviews RENAME TO provider_reviews;

ALTER TABLE reviewer_info RENAME TO review_reviewer_info;

ALTER TABLE overall_provider_scores RENAME TO provider_review_scores;

ALTER TABLE bad_review_records RENAME TO invalid_provider_reviews;
