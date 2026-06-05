package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.PropertyRecommendationView;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.persistence.SearchHistoryRepository;
import com.plovdiv.advisor.service.MapService;
import com.plovdiv.advisor.service.RecommendationException;
import com.plovdiv.advisor.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final RecommendationService recommendationService;
    private final OntologyService ontologyService;
    private final MapService mapService;
    private final SearchHistoryRepository searchHistoryRepository;

    @ModelAttribute("profiles")
    public BuyerProfile[] profiles() {
        return BuyerProfile.values();
    }

    @ModelAttribute("districts")
    public District[] districts() {
        return District.values();
    }

    @ModelAttribute("constructionTypes")
    public ConstructionType[] constructionTypes() {
        return ConstructionType.values();
    }

    @ModelAttribute("heatingTypes")
    public HeatingType[] heatingTypes() {
        return HeatingType.values();
    }

    @ModelAttribute("priorities")
    public List<String> priorities() {
        return List.of("School", "Kindergarten", "University", "Park", "Pharmacy", "Hospital", "TransportStop");
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("projectName", "Smart Real Estate Advisor for Plovdiv");
        model.addAttribute("searchForm", new SearchForm());
        return "index";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute SearchForm searchForm, Model model) {
        SearchCriteria criteria = searchForm.toCriteria();
        model.addAttribute("projectName", "Smart Real Estate Advisor for Plovdiv");
        model.addAttribute("searchForm", searchForm);

        try {
            searchHistoryRepository.save(criteria);
            List<PropertyRecommendationView> recommendations = recommendationService.search(criteria).stream()
                    .map(this::toView)
                    .flatMap(Optional::stream)
                    .toList();
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("mapMarkers", mapService.markersForRecommendations(recommendations));
        } catch (RecommendationException ex) {
            model.addAttribute("recommendations", List.of());
            model.addAttribute("mapMarkers", List.of());
            model.addAttribute("searchError", ex.getMessage());
        }

        return "search/results";
    }

    private Optional<PropertyRecommendationView> toView(RecommendationResult result) {
        return ontologyService.findProperty(result.propertyId())
                .map(property -> new PropertyRecommendationView(
                        property,
                        result.score(),
                        result.confidence(),
                        result.explanations()
                ));
    }
}
