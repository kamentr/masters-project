package com.plovdiv.advisor.persistence;

import com.plovdiv.advisor.dto.FeedbackEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class FeedbackRepository {
    private final JdbcTemplate jdbcTemplate;

    public FeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String propertyId, int rating, String comment, boolean useful) {
        jdbcTemplate.update(
                "INSERT INTO feedback (property_id, rating, comment, useful, created_at) VALUES (?, ?, ?, ?, ?)",
                propertyId,
                rating,
                comment,
                useful ? 1 : 0,
                Instant.now().toString()
        );
    }

    public List<FeedbackEntry> findByPropertyId(String propertyId) {
        return jdbcTemplate.query(
                """
                        SELECT id, user_id, property_id, rating, comment, useful, created_at
                        FROM feedback
                        WHERE property_id = ?
                        ORDER BY created_at DESC
                        """,
                this::mapFeedback,
                propertyId
        );
    }

    private FeedbackEntry mapFeedback(ResultSet rs, int rowNum) throws SQLException {
        Long userId = rs.getObject("user_id") == null ? null : rs.getLong("user_id");
        return new FeedbackEntry(
                rs.getLong("id"),
                userId,
                rs.getString("property_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getInt("useful") == 1,
                Instant.parse(rs.getString("created_at"))
        );
    }
}
