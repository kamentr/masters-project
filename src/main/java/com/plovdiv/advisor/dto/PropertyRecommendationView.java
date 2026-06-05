package com.plovdiv.advisor.dto;

import com.plovdiv.advisor.ontology.PropertyOntologyRecord;

import java.util.List;

public record PropertyRecommendationView(
        PropertyOntologyRecord property,
        int score,
        Confidence confidence,
        List<String> explanations
) {
}
