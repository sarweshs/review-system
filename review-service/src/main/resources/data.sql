-- Sample data for-- Table: providers
                  CREATE TABLE providers (
                      provider_id INT PRIMARY KEY,
                      provider_name VARCHAR(100)
                  );

                  -- Table: hotels
                  CREATE TABLE hotels (
                      hotel_id INT PRIMARY KEY,
                      platform VARCHAR(50),
                      hotel_name VARCHAR(255)
                  );

                  -- Table: hotel_reviews
                  CREATE TABLE hotel_reviews (
                      hotel_review_id BIGINT PRIMARY KEY,
                      hotel_id INT,
                      provider_id INT,
                      rating FLOAT,
                      formatted_rating VARCHAR(10),
                      rating_text VARCHAR(50),
                      review_title TEXT,
                      review_comments TEXT,
                      review_positives TEXT,
                      review_negatives TEXT,
                      review_date DATETIME,
                      check_in_month_year VARCHAR(20),
                      review_provider_logo TEXT,
                      review_provider_text VARCHAR(100),
                      encrypted_review_data TEXT,
                      is_show_review_response BOOLEAN,
                      response_date_text VARCHAR(50),
                      formatted_review_date VARCHAR(50),
                      responder_name VARCHAR(255),
                      translate_source VARCHAR(10),
                      translate_target VARCHAR(10),
                      original_title TEXT,
                      original_comment TEXT,
                      formatted_response_date VARCHAR(50),
                      FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id),
                      FOREIGN KEY (provider_id) REFERENCES providers(provider_id)
                  );

                  -- Table: reviewers
                  CREATE TABLE reviewers (
                      hotel_review_id BIGINT PRIMARY KEY,
                      country_id INT,
                      country_name VARCHAR(100),
                      flag_name VARCHAR(10),
                      display_member_name VARCHAR(100),
                      review_group_id INT,
                      review_group_name VARCHAR(100),
                      room_type_id INT,
                      room_type_name VARCHAR(100),
                      length_of_stay INT,
                      reviewer_reviewed_count INT,
                      is_expert_reviewer BOOLEAN,
                      is_show_global_icon BOOLEAN,
                      is_show_reviewed_count BOOLEAN,
                      FOREIGN KEY (hotel_review_id) REFERENCES hotel_reviews(hotel_review_id)
                  );

                  -- Table: overall_scores
                  CREATE TABLE overall_scores (
                      hotel_id INT,
                      provider_id INT,
                      overall_score FLOAT,
                      review_count INT,
                      PRIMARY KEY (hotel_id, provider_id),
                      FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id),
                      FOREIGN KEY (provider_id) REFERENCES providers(provider_id)
                  );

                  -- Table: overall_score_grades
                  CREATE TABLE overall_score_grades (
                      hotel_id INT,
                      provider_id INT,
                      category VARCHAR(100),
                      score FLOAT,
                      PRIMARY KEY (hotel_id, provider_id, category),
                      FOREIGN KEY (hotel_id, provider_id) REFERENCES overall_scores(hotel_id, provider_id)
                  ); reviews table