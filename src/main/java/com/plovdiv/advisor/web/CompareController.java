package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.PropertyRecommendationView;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.persistence.FavoriteRepository;
import com.plovdiv.advisor.persistence.SearchHistoryRepository;
import com.plovdiv.advisor.service.RecommendationScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CompareController {

    private final OntologyService ontologyService;
    private final FavoriteRepository favoriteRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final RecommendationScoringService scoringService;

    @GetMapping("/compare")
    public String compare(@RequestParam(value = "ids", required = false) List<String> ids, Model model) {
        if (ids == null) {
            ids = Collections.emptyList();
        }

        List<String> finalIds = ids.stream().distinct().limit(4).toList();

        SearchCriteria criteria = searchHistoryRepository.findLatest()
                .orElseGet(() -> new SearchCriteria(
                        BuyerProfile.FAMILY,
                        new BigDecimal("200000"),
                        Collections.emptyList(),
                        0,
                        0,
                        null,
                        null,
                        false,
                        false,
                        false,
                        Collections.emptyList()
                ));

        List<PropertyRecommendationView> properties = new ArrayList<>();
        for (String id : finalIds) {
            Optional<PropertyOntologyRecord> recordOpt = ontologyService.findProperty(id);
            if (recordOpt.isPresent()) {
                PropertyOntologyRecord record = recordOpt.get();
                int baseScore = scoringService.baseScore(record, criteria);
                int neighborhoodScore = scoringService.neighborhoodScore(record, criteria.profile());
                int score = Math.min(100, baseScore + neighborhoodScore);
                
                properties.add(new PropertyRecommendationView(
                        record,
                        score,
                        Confidence.HIGH,
                        Collections.emptyList()
                ));
            }
        }

        model.addAttribute("properties", properties);
        model.addAttribute("profile", criteria.profile());
        return "properties/compare";
    }

    @PostMapping("/properties/{id}/favorite")
    public String toggleFavorite(
            @PathVariable String id,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl) {
        if (favoriteRepository.isFavorite(id)) {
            favoriteRepository.removeFavorite(id);
        } else {
            favoriteRepository.addFavorite(id);
        }
        
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/properties/" + id;
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        List<String> favIds = favoriteRepository.getFavoritePropertyIds();
        List<PropertyOntologyRecord> properties = new ArrayList<>();
        for (String id : favIds) {
            ontologyService.findProperty(id).ifPresent(properties::add);
        }
        model.addAttribute("properties", properties);
        return "properties/favorites";
    }
}
