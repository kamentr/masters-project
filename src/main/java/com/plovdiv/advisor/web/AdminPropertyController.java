package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.ImportBatchResult;
import com.plovdiv.advisor.dto.PropertyType;
import com.plovdiv.advisor.service.AgentLogService;
import com.plovdiv.advisor.service.PropertyImportService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.plovdiv.advisor.ontology.OntologyService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminPropertyController {

    private final PropertyImportService propertyImportService;
    private final AgentLogService agentLogService;
    private final OntologyService ontologyService;

    public AdminPropertyController(
            PropertyImportService propertyImportService,
            AgentLogService agentLogService,
            OntologyService ontologyService) {
        this.propertyImportService = propertyImportService;
        this.agentLogService = agentLogService;
        this.ontologyService = ontologyService;
    }

    @ModelAttribute
    void referenceData(Model model) {
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("districts", District.values());
        model.addAttribute("constructionTypes", ConstructionType.values());
        model.addAttribute("heatingTypes", HeatingType.values());
    }

    @GetMapping("/properties")
    public String properties(Model model) {
        model.addAttribute("properties", propertyImportService.listProperties());
        return "admin/properties";
    }

    @GetMapping("/properties/{id}/edit")
    public String editProperty(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return propertyImportService.findEditForm(id)
                .map(form -> {
                    model.addAttribute("propertyEditForm", form);
                    return "admin/property-edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Property " + id + " was not found.");
                    return "redirect:/admin/properties";
                });
    }

    @PostMapping("/properties/{id}/edit")
    public String updateProperty(
            @PathVariable String id,
            @Valid @ModelAttribute PropertyEditForm propertyEditForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        propertyEditForm.setId(id);
        if (!propertyEditForm.hasConsistentFloors()) {
            bindingResult.rejectValue("floor", "floor.invalid", "Floor must be less than or equal to total floors.");
        }
        if (bindingResult.hasErrors()) {
            return "admin/property-edit";
        }

        try {
            propertyImportService.updateProperty(propertyEditForm);
            redirectAttributes.addFlashAttribute("success", "Property " + id + " was updated.");
            return "redirect:/admin/properties";
        } catch (RuntimeException ex) {
            bindingResult.reject("ontology.update.failed", ex.getMessage());
            return "admin/property-edit";
        }
    }

    @PostMapping("/properties/{id}/unavailable")
    public String markUnavailable(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            propertyImportService.markUnavailable(id);
            redirectAttributes.addFlashAttribute("success", "Property " + id + " was marked unavailable.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/properties";
    }

    @GetMapping("/import")
    public String importPage() {
        return "admin/import";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Choose a CSV file to import.");
            return "admin/import";
        }

        ImportBatchResult result = propertyImportService.importCsv(file);
        model.addAttribute("result", result);
        if (result.successful()) {
            model.addAttribute("success", "Imported " + result.importedRows() + " properties.");
        } else {
            model.addAttribute("error", result.errorSummary());
        }
        return "admin/import";
    }

    @GetMapping("/agents")
    public String agentLogs(
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "limit", required = false) Integer limit,
            Model model) {
        String trimmedRequestId = requestId == null ? "" : requestId.trim();
        model.addAttribute("requestId", trimmedRequestId);
        model.addAttribute("limit", limit == null ? 100 : limit);
        model.addAttribute("logs", trimmedRequestId.isBlank()
                ? agentLogService.recentLogs(limit)
                : agentLogService.logsForRequest(trimmedRequestId));
        return "admin/agents";
    }

    @GetMapping("/properties/new")
    public String newPropertyForm(Model model) {
        model.addAttribute("propertyEditForm", new PropertyEditForm());
        return "admin/property-new";
    }

    @PostMapping("/properties/new")
    public String createProperty(
            @Valid @ModelAttribute PropertyEditForm propertyEditForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (propertyEditForm.getId() != null && !propertyEditForm.getId().isBlank()) {
            if (ontologyService.findProperty(propertyEditForm.getId()).isPresent()) {
                bindingResult.rejectValue("id", "id.exists", "Property ID already exists.");
            }
        }
        if (!propertyEditForm.hasConsistentFloors()) {
            bindingResult.rejectValue("floor", "floor.invalid", "Floor must be less than or equal to total floors.");
        }
        if (bindingResult.hasErrors()) {
            return "admin/property-new";
        }

        try {
            propertyImportService.updateProperty(propertyEditForm);
            redirectAttributes.addFlashAttribute("success", "Property " + propertyEditForm.getId() + " was created manually.");
            return "redirect:/admin/properties";
        } catch (RuntimeException ex) {
            bindingResult.reject("ontology.create.failed", ex.getMessage());
            return "admin/property-new";
        }
    }
}
