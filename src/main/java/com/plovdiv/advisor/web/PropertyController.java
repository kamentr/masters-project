package com.plovdiv.advisor.web;

import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.service.FeedbackService;
import com.plovdiv.advisor.service.MapService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PropertyController {
    private final OntologyService ontologyService;
    private final FeedbackService feedbackService;
    private final MapService mapService;

    public PropertyController(OntologyService ontologyService, FeedbackService feedbackService, MapService mapService) {
        this.ontologyService = ontologyService;
        this.feedbackService = feedbackService;
        this.mapService = mapService;
    }

    @GetMapping("/properties/{id}")
    public String detail(@PathVariable String id, Model model) {
        PropertyOntologyRecord property = ontologyService.findProperty(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

        model.addAttribute("property", property);
        model.addAttribute("feedback", feedbackService.findByPropertyId(id));
        model.addAttribute("feedbackForm", new FeedbackForm());
        model.addAttribute("mapMarker", mapService.markerFor(property, 0));
        return "properties/detail";
    }

    @PostMapping("/properties/{id}/feedback")
    public String saveFeedback(@PathVariable String id, @ModelAttribute FeedbackForm feedbackForm) {
        if (ontologyService.findProperty(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }
        feedbackService.save(id, feedbackForm.getRating(), feedbackForm.getComment(), feedbackForm.isUseful());
        return "redirect:/properties/" + id;
    }
}
