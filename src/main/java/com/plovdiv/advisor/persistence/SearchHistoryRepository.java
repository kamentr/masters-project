package com.plovdiv.advisor.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plovdiv.advisor.dto.SearchCriteria;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class SearchHistoryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SearchHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public void save(SearchCriteria criteria) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO search_history (criteria_json, selected_profile, created_at) VALUES (?, ?, ?)",
                    objectMapper.writeValueAsString(criteria),
                    criteria.profile() == null ? "" : criteria.profile().name(),
                    Instant.now().toString()
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize search criteria", ex);
        }
    }

    public Optional<SearchCriteria> findLatest() {
        List<SearchCriteria> list = jdbcTemplate.query(
                "SELECT criteria_json FROM search_history ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> {
                    try {
                        return objectMapper.readValue(rs.getString("criteria_json"), SearchCriteria.class);
                    } catch (Exception e) {
                        return null;
                    }
                }
        );
        return list.isEmpty() || list.get(0) == null ? Optional.empty() : Optional.of(list.get(0));
    }
}
