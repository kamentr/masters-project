package com.plovdiv.advisor.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FavoriteRepository {
    private final JdbcTemplate jdbcTemplate;

    public FavoriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addFavorite(String propertyId) {
        jdbcTemplate.update(
                "INSERT OR IGNORE INTO favorites (user_id, property_id) VALUES (1, ?)",
                propertyId
        );
    }

    public void removeFavorite(String propertyId) {
        jdbcTemplate.update(
                "DELETE FROM favorites WHERE user_id = 1 AND property_id = ?",
                propertyId
        );
    }

    public List<String> getFavoritePropertyIds() {
        return jdbcTemplate.queryForList(
                "SELECT property_id FROM favorites WHERE user_id = 1",
                String.class
        );
    }

    public boolean isFavorite(String propertyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE user_id = 1 AND property_id = ?",
                Integer.class,
                propertyId
        );
        return count != null && count > 0;
    }
}
