package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.PropertyRecommendationView;
import com.plovdiv.advisor.dto.PropertyType;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MapServiceTests {

    private final MapService mapService = new MapService();

    @Test
    void createsMarkersFromRecommendationViews() {
        PropertyOntologyRecord property = property("P777");
        List<PropertyRecommendationView> recommendations = List.of(
                new PropertyRecommendationView(property, 91, Confidence.HIGH, List.of("Within budget"))
        );

        var markers = mapService.markersForRecommendations(recommendations);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().propertyId()).isEqualTo("P777");
        assertThat(markers.getFirst().district()).isEqualTo("Trakia");
        assertThat(markers.getFirst().score()).isEqualTo(91);
    }

    private PropertyOntologyRecord property(String id) {
        return new PropertyOntologyRecord(
                id,
                "Test apartment",
                PropertyType.APARTMENT,
                District.TRAKIA,
                new BigDecimal("125000"),
                new BigDecimal("80"),
                new BigDecimal("1562.50"),
                3,
                2,
                4,
                8,
                ConstructionType.BRICK,
                2018,
                HeatingType.ELECTRIC,
                true,
                true,
                false,
                true,
                new BigDecimal("42.1354"),
                new BigDecimal("24.7854"),
                500,
                450,
                1200,
                700,
                300,
                1600,
                400,
                Set.of("FamilyProfile")
        );
    }
}
