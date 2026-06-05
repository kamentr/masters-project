package com.plovdiv.advisor.dto;

import java.util.List;

public record RecommendationResult(
        String propertyId,
        int score,
        Confidence confidence,
        List<String> explanations
) {
}
