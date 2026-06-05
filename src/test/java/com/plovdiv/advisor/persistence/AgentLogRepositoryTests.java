package com.plovdiv.advisor.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentLogRepositoryTests {

    @Autowired
    private AgentLogRepository agentLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearLogs() {
        jdbcTemplate.execute("DELETE FROM agent_logs");
    }

    @Test
    void findsRecentLogsWithLimit() {
        agentLogRepository.save("req-1", "UserRequestAgent", "RecommendationAgent", "REQUEST", "first");
        agentLogRepository.save("req-2", "RecommendationAgent", "UserRequestAgent", "INFORM", "second");

        var logs = agentLogRepository.findRecent(1);

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().messageSummary()).isEqualTo("second");
    }

    @Test
    void findsLogsForSingleRequestInMessageOrder() {
        agentLogRepository.save("req-1", "UserRequestAgent", "RecommendationAgent", "REQUEST", "request sent");
        agentLogRepository.save("req-2", "RecommendationAgent", "PropertyAgent", "REQUEST", "other request");
        agentLogRepository.save("req-1", "RecommendationAgent", "UserRequestAgent", "INFORM", "response received");

        var logs = agentLogRepository.findByRequestId("req-1");

        assertThat(logs).extracting("messageSummary")
                .containsExactly("request sent", "response received");
    }
}
