package com.plovdiv.advisor.agent;

import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.OntologyUpdateCommand;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import com.plovdiv.advisor.service.PropertyCsvParser;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AgentAclIntegrationTests {

    @Autowired
    private AgentBridge agentBridge;

    @Autowired
    private JadeManager jadeManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private AgentLogRepository agentLogRepository;

    @Autowired
    private PropertyCsvParser csvParser;

    @BeforeEach
    void setupOntologyAndLogs() throws Exception {
        jdbcTemplate.execute("DELETE FROM agent_logs");

        // Parse and seed ontology with synthetic listings
        List<PropertyImportRow> rows = csvParser.parse(new ClassPathResource("data/properties-plovdiv.csv"));
        ontologyService.upsertProperties(rows);
        ontologyService.save();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSuccessfulSearchFlow() throws Exception {
        String requestId = UUID.randomUUID().toString();
        SearchCriteria criteria = new SearchCriteria(
                BuyerProfile.FAMILY,
                new BigDecimal("250000"),
                List.of(District.TRAKIA),
                2,
                1,
                null,
                null,
                false,
                false,
                false,
                List.of("School", "Kindergarten")
        );

        AgentMessage<SearchCriteria> request = new AgentMessage<>(requestId, "SEARCH_PROPERTIES", criteria);
        CompletableFuture<AgentMessage<?>> future = new CompletableFuture<>();
        agentBridge.registerRequest(requestId, future);

        // Send request through Spring-to-JADE bridge
        agentBridge.sendRequest(request);

        // Wait for response from UserRequestAgent
        AgentMessage<?> response = future.get(8, TimeUnit.SECONDS);

        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getErrors()).isEmpty();

        List<?> payloadRaw = (List<?>) response.getPayload();
        assertThat(payloadRaw).isNotEmpty();

        // Check if logs are written to SQLite agent_logs table
        List<Map<String, Object>> logs = jdbcTemplate.queryForList("SELECT * FROM agent_logs WHERE request_id = ?", requestId);
        assertThat(logs).isNotEmpty();
        
        // Assert that logs contain messages from UserRequestAgent, RecommendationAgent, PropertyAgent, NeighborhoodAgent
        boolean hasUserReq = logs.stream().anyMatch(l -> "UserRequestAgent".equals(l.get("sender")));
        boolean hasRec = logs.stream().anyMatch(l -> "RecommendationAgent".equals(l.get("sender")));
        boolean hasProp = logs.stream().anyMatch(l -> "PropertyAgent".equals(l.get("sender")));
        boolean hasNh = logs.stream().anyMatch(l -> "NeighborhoodAgent".equals(l.get("sender")));

        assertThat(hasUserReq).isTrue();
        assertThat(hasRec).isTrue();
        assertThat(hasProp).isTrue();
        assertThat(hasNh).isTrue();
    }

    @Test
    void testNeighborhoodAgentFallback() throws Exception {
        ContainerController container = jadeManager.getMainContainer();
        AgentController nhAgent = container.getAgent("NeighborhoodAgent");
        try {
            // Kill NeighborhoodAgent to simulate its timeout/failure
            nhAgent.kill();

            String requestId = UUID.randomUUID().toString();
            SearchCriteria criteria = new SearchCriteria(
                    BuyerProfile.FAMILY,
                    new BigDecimal("250000"),
                    List.of(District.TRAKIA),
                    2,
                    1,
                    null,
                    null,
                    false,
                    false,
                    false,
                    List.of("School", "Kindergarten")
            );

            AgentMessage<SearchCriteria> request = new AgentMessage<>(requestId, "SEARCH_PROPERTIES", criteria);
            CompletableFuture<AgentMessage<?>> future = new CompletableFuture<>();
            agentBridge.registerRequest(requestId, future);

            agentBridge.sendRequest(request);

            // Wait for response (includes timeout fallback)
            AgentMessage<?> response = future.get(8, TimeUnit.SECONDS);

            assertThat(response.getRequestId()).isEqualTo(requestId);
            List<?> results = (List<?>) response.getPayload();
            assertThat(results).isNotEmpty();

            // Verify fallback structure: check if results returned are not empty and first result's explanation indicates fallback
            com.fasterxml.jackson.databind.ObjectMapper mapper = AgentUtils.getMapper();
            RecommendationResult firstResult = mapper.convertValue(results.get(0), RecommendationResult.class);
            
            assertThat(firstResult.confidence()).isEqualTo(Confidence.MEDIUM);
            assertThat(firstResult.explanations()).contains("Lifestyle suitability scoring was unavailable");
        } finally {
            // Respawn NeighborhoodAgent to keep JADE state clean for subsequent tests/runs
            AgentController ac = container.createNewAgent("NeighborhoodAgent", NeighborhoodAgent.class.getName(), new Object[]{ontologyService, agentLogRepository});
            ac.start();
            // Wait briefly for agent to setup
            Thread.sleep(500);
        }
    }

    @Test
    void testPropertyAgentFailure() throws Exception {
        ContainerController container = jadeManager.getMainContainer();
        AgentController propAgent = container.getAgent("PropertyAgent");
        try {
            // Kill PropertyAgent to simulate its failure
            propAgent.kill();

            String requestId = UUID.randomUUID().toString();
            SearchCriteria criteria = new SearchCriteria(
                    BuyerProfile.FAMILY,
                    new BigDecimal("250000"),
                    List.of(District.TRAKIA),
                    2,
                    1,
                    null,
                    null,
                    false,
                    false,
                    false,
                    new ArrayList<>()
            );

            AgentMessage<SearchCriteria> request = new AgentMessage<>(requestId, "SEARCH_PROPERTIES", criteria);
            CompletableFuture<AgentMessage<?>> future = new CompletableFuture<>();
            agentBridge.registerRequest(requestId, future);

            agentBridge.sendRequest(request);

            // Future should complete exceptionally due to timeout reply
            assertThatThrownBy(() -> future.get(8, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(RuntimeException.class);
        } finally {
            // Respawn PropertyAgent
            AgentController ac = container.createNewAgent("PropertyAgent", PropertyAgent.class.getName(), new Object[]{ontologyService, agentLogRepository});
            ac.start();
            // Wait briefly for agent to setup
            Thread.sleep(500);
        }
    }

    @Test
    void testOntologyUpdateAgent() throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        // Find an existing property ID to change price
        List<String> propertyIds = ontologyService.findAllPropertyIds();
        assertThat(propertyIds).isNotEmpty();
        String targetId = propertyIds.get(0);

        PropertyOntologyRecord originalRecord = ontologyService.findProperty(targetId).orElseThrow();
        BigDecimal newPrice = originalRecord.priceEUR().add(new BigDecimal("10000"));

        OntologyUpdateCommand cmd = new OntologyUpdateCommand(
                "UPDATE_PRICE",
                null,
                targetId,
                newPrice,
                null
        );

        AgentMessage<OntologyUpdateCommand> request = new AgentMessage<>(requestId, "UPDATE_ONTOLOGY", cmd);
        CompletableFuture<AgentMessage<?>> future = new CompletableFuture<>();
        agentBridge.registerRequest(requestId, future);

        // Send request
        agentBridge.sendRequest(request);

        // Wait for response
        AgentMessage<?> response = future.get(8, TimeUnit.SECONDS);

        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getPayload().toString()).contains("updated successfully");

        // Verify updated in ontology
        PropertyOntologyRecord updatedRecord = ontologyService.findProperty(targetId).orElseThrow();
        assertThat(updatedRecord.priceEUR()).isEqualByComparingTo(newPrice);
    }
}
