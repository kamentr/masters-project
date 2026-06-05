package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.NeighborhoodScore;
import com.plovdiv.advisor.dto.PropertyCandidate;
import com.plovdiv.advisor.dto.PropertyType;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationScoringServiceTests {

    private final RecommendationScoringService scoringService = new RecommendationScoringService();

    @Test
    void filtersOutUnavailableAndOverBudgetProperties() {
        SearchCriteria criteria = familyCriteria(new BigDecimal("150000"));

        assertThat(scoringService.candidateFor(record("P001", true, new BigDecimal("140000"), 2), criteria))
                .isPresent();
        assertThat(scoringService.candidateFor(record("P002", false, new BigDecimal("140000"), 2), criteria))
                .isEmpty();
        assertThat(scoringService.candidateFor(record("P003", true, new BigDecimal("170000"), 2), criteria))
                .isEmpty();
    }

    @Test
    void appliesFamilyProfileAmenityScore() {
        PropertyOntologyRecord record = record("P001", true, new BigDecimal("140000"), 2);

        assertThat(scoringService.neighborhoodScore(record, BuyerProfile.FAMILY)).isEqualTo(35);
    }

    @Test
    void ranksByScoreThenPricePerSquareMeterThenPriorityDistanceThenYear() {
        PropertyOntologyRecord lowerPricePerSqM = record("P001", true, new BigDecimal("140000"), 2);
        PropertyOntologyRecord higherPricePerSqM = record("P002", true, new BigDecimal("150000"), 2);
        OntologyService ontologyService = mock(OntologyService.class);
        when(ontologyService.findProperty("P001")).thenReturn(Optional.of(lowerPricePerSqM));
        when(ontologyService.findProperty("P002")).thenReturn(Optional.of(higherPricePerSqM));

        List<RecommendationResult> results = scoringService.combineAndRank(
                familyCriteria(new BigDecimal("200000")),
                List.of(new PropertyCandidate("P002", 50), new PropertyCandidate("P001", 50)),
                List.of(new NeighborhoodScore("P002", 20), new NeighborhoodScore("P001", 20)),
                Confidence.HIGH,
                false,
                ontologyService
        );

        assertThat(results).extracting(RecommendationResult::propertyId).containsExactly("P001", "P002");
        assertThat(results.getFirst().score()).isEqualTo(70);
        assertThat(results.getFirst().explanations()).contains("2 bedrooms", "333m from school");
    }

    private SearchCriteria familyCriteria(BigDecimal budget) {
        return new SearchCriteria(
                BuyerProfile.FAMILY,
                budget,
                List.of(District.CENTER),
                2,
                2,
                null,
                null,
                false,
                false,
                false,
                List.of("School")
        );
    }

    private PropertyOntologyRecord record(String id, boolean available, BigDecimal priceEUR, int bedrooms) {
        BigDecimal area = new BigDecimal("100");
        return new PropertyOntologyRecord(
                id,
                "Test property " + id,
                PropertyType.APARTMENT,
                District.CENTER,
                priceEUR,
                area,
                priceEUR.divide(area),
                3,
                bedrooms,
                2,
                6,
                ConstructionType.BRICK,
                2020,
                HeatingType.ELECTRIC,
                available,
                true,
                true,
                true,
                new BigDecimal("42.14"),
                new BigDecimal("24.75"),
                333,
                291,
                397,
                239,
                163,
                737,
                211,
                Set.of("FamilyProfile")
        );
    }
}
