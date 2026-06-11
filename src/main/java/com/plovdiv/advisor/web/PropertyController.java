package com.plovdiv.advisor.web;

import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class PropertyController {
    private final OntologyService ontologyService;
    private final MapService mapService;

    @GetMapping("/properties/{id}")
    public String detail(@PathVariable String id, Model model) {
        PropertyOntologyRecord property = ontologyService.findProperty(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

        model.addAttribute("property", property);
        model.addAttribute("mapMarker", mapService.markerFor(property, 0));
        return "properties/detail";
    }
}
