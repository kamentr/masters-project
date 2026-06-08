package com.plovdiv.advisor.service;

import com.plovdiv.advisor.agent.AgentBridge;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RecommendationServiceTests {

    @Test
    void sendsSearchRequestThroughAgentBridge() {
        AgentBridge bridge = mock(AgentBridge.class);
        RecommendationService service = new RecommendationService(bridge);

        doAnswer(invocation -> {
            AgentMessage<SearchCriteria> request = invocation.getArgument(0);
            RecommendationResult result = new RecommendationResult("P001", 92, Confidence.HIGH, List.of("Within budget"));
            return CompletableFuture.completedFuture(
                    new AgentMessage<>(request.getRequestId(), "RECOMMENDATION_RESULTS", List.of(result)));
        }).when(bridge).sendRequest(any(), any());

        List<RecommendationResult> results = service.search(new SearchCriteria(
                BuyerProfile.FAMILY,
                new BigDecimal("150000"),
                List.of(),
                2,
                2,
                null,
                null,
                false,
                false,
                false,
                List.of("School")
        ));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().propertyId()).isEqualTo("P001");
        assertThat(results.getFirst().score()).isEqualTo(92);
    }
}
