package com.reviewservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db/migration/V1__create_reviews_table.sql")
                .addScript("classpath:db/migration/V2__create_review_sources_table.sql")
                .addScript("classpath:db/migration/V3__add_source_to_reviews.sql")
                .addScript("classpath:db/migration/V4__add_credential_to_reviews.sql")
                .addScript("classpath:db/migration/V5__remove_backend_from_review_sources.sql")
                .build();
    }
} 