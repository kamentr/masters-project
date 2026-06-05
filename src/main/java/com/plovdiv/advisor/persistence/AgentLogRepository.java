package com.plovdiv.advisor.persistence;

import com.plovdiv.advisor.dto.AgentLogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class AgentLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AgentLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String requestId, String sender, String receiver, String performative, String messageSummary) {
        String sql = "INSERT INTO agent_logs (request_id, sender, receiver, performative, message_summary, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, requestId, sender, receiver, performative, messageSummary, Instant.now().toString());
    }

    public List<AgentLogEntry> findRecent(int limit) {
        return jdbcTemplate.query(
                """
                        SELECT id, request_id, sender, receiver, performative, message_summary, created_at
                        FROM agent_logs
                        ORDER BY datetime(created_at) DESC, id DESC
                        LIMIT ?
                        """,
                this::mapEntry,
                Math.max(1, Math.min(500, limit))
        );
    }

    public List<AgentLogEntry> findByRequestId(String requestId) {
        return jdbcTemplate.query(
                """
                        SELECT id, request_id, sender, receiver, performative, message_summary, created_at
                        FROM agent_logs
                        WHERE request_id = ?
                        ORDER BY datetime(created_at) ASC, id ASC
                        """,
                this::mapEntry,
                requestId
        );
    }

    private AgentLogEntry mapEntry(ResultSet rs, int rowNum) throws SQLException {
        return new AgentLogEntry(
                rs.getLong("id"),
                rs.getString("request_id"),
                rs.getString("sender"),
                rs.getString("receiver"),
                rs.getString("performative"),
                rs.getString("message_summary"),
                Instant.parse(rs.getString("created_at"))
        );
    }
}
