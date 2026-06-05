package com.plovdiv.advisor.service;

import com.plovdiv.advisor.agent.AgentBridge;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class RecommendationService {

    private static final long AGENT_TIMEOUT_SECONDS = 8;

    private final AgentBridge agentBridge;

    public RecommendationService(AgentBridge agentBridge) {
        this.agentBridge = agentBridge;
    }

    public List<RecommendationResult> search(SearchCriteria criteria) {
        String requestId = UUID.randomUUID().toString();
        AgentMessage<SearchCriteria> request = new AgentMessage<>(requestId, "SEARCH_PROPERTIES", criteria);
        CompletableFuture<AgentMessage<?>> future = new CompletableFuture<>();
        agentBridge.registerRequest(requestId, future);
        agentBridge.sendRequest(request);

        try {
            AgentMessage<?> response = future.get(AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                throw new RecommendationException(String.join("; ", response.getErrors()));
            }
            return AgentUtilsForRecommendation.convertResults(response.getPayload());
        } catch (RecommendationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RecommendationException("Recommendation request failed: " + ex.getMessage(), ex);
        }
    }
}
