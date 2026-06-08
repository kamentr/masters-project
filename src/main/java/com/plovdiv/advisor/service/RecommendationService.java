package com.plovdiv.advisor.service;

import com.plovdiv.advisor.agent.AgentBridge;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Duration AGENT_TIMEOUT = Duration.ofSeconds(8);

    private final AgentBridge agentBridge;

    public List<RecommendationResult> search(SearchCriteria criteria) {
        var request = new AgentMessage<>(UUID.randomUUID().toString(), "SEARCH_PROPERTIES", criteria);

        try {
            AgentMessage<?> response = agentBridge.sendRequest(request, AGENT_TIMEOUT).get();
            if (response.hasErrors()) {
                throw new RecommendationException(response.errorSummary());
            }
            return AgentUtilsForRecommendation.convertResults(response.getPayload());
        } catch (RecommendationException ex) {
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new RecommendationException("Recommendation request failed: " + cause.getMessage(), cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RecommendationException("Recommendation request was interrupted", ex);
        }
    }
}
