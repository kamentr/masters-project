package com.plovdiv.advisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plovdiv.advisor.dto.RecommendationResult;

import java.util.List;

final class AgentUtilsForRecommendation {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private AgentUtilsForRecommendation() {
    }

    static List<RecommendationResult> convertResults(Object payload) {
        if (payload == null) {
            return List.of();
        }
        return MAPPER.convertValue(
                payload,
                MAPPER.getTypeFactory().constructCollectionType(List.class, RecommendationResult.class)
        );
    }
}
