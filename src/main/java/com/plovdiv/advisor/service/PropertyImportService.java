package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.ImportBatchResult;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.persistence.ImportBatchRepository;
import com.plovdiv.advisor.web.PropertyEditForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropertyImportService {

    private final PropertyCsvParser csvParser;
    private final ImportBatchRepository importBatchRepository;
    private final OntologyService ontologyService;

    public ImportBatchResult importCsv(MultipartFile file) {
        String fileName = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "uploaded-properties.csv";
        ImportBatchResult pending = importBatchRepository.create(fileName);

        List<PropertyImportRow> rows;
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            rows = csvParser.parse(reader);
        } catch (CsvImportValidationException ex) {
            return importBatchRepository.fail(pending.batchId(), 0, String.join("; ", ex.errors()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        try {
            ontologyService.upsertProperties(rows);
            ontologyService.save();
            return importBatchRepository.complete(pending.batchId(), rows.size(), rows.size());
        } catch (RuntimeException ex) {
            return importBatchRepository.fail(pending.batchId(), rows.size(), ex.getMessage());
        }
    }

    public List<PropertyOntologyRecord> listProperties() {
        return ontologyService.findAllPropertyIds().stream()
                .map(ontologyService::findProperty)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(PropertyOntologyRecord::id))
                .toList();
    }

    public Optional<PropertyEditForm> findEditForm(String propertyId) {
        return ontologyService.findProperty(propertyId).map(PropertyEditForm::fromRecord);
    }

    public void updateProperty(PropertyEditForm form) {
        ontologyService.upsertProperty(form.toImportRow());
        ontologyService.save();
    }

    public void markUnavailable(String propertyId) {
        ontologyService.updateAvailability(propertyId, false);
        ontologyService.save();
    }
}
