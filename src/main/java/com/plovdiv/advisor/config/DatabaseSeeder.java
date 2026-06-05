package com.plovdiv.advisor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count == null || count == 0) {
            logger.info("No users found. Seeding default demo user...");
            jdbcTemplate.update("INSERT INTO users (id, display_name) VALUES (1, 'Demo User')");
            logger.info("Demo user seeded successfully.");
        }
    }
}
