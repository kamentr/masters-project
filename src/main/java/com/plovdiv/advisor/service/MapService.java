package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.MapMarker;
import com.plovdiv.advisor.dto.PropertyRecommendationView;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MapService {

    public List<MapMarker> markersForRecommendations(List<PropertyRecommendationView> recommendations) {
        return recommendations.stream()
                .map(view -> markerFor(view.property(), view.score()))
                .toList();
    }

    public MapMarker markerFor(PropertyOntologyRecord property, int score) {
        return new MapMarker(
                property.id(),
                property.title(),
                property.district().csvValue(),
                property.latitude(),
                property.longitude(),
                score
        );
    }
}
