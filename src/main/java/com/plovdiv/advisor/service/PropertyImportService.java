package com.plovdiv.advisor.service;

import com.plovdiv.advisor.agent.AgentBridge;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.ImportBatchResult;
import com.plovdiv.advisor.dto.OntologyUpdateCommand;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import com.plovdiv.advisor.persistence.ImportBatchRepository;
import com.plovdiv.advisor.web.PropertyEditForm;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PropertyImportService {

    private static final long AGENT_TIMEOUT_SECONDS = 8;

    private final PropertyCsvParser csvParser;
    private final ImportBatchRepository importBatchRepository;
    private final OntologyService ontologyService;
    private final AgentBridge agentBridge;

    public PropertyImportService(
            PropertyCsvParser csvParser,
            ImportBatchRepository importBatchRepository,
            OntologyService ontologyService,
            AgentBridge agentBridge) {
        this.csvParser = csvParser;
        this.importBatchRepository = importBatchRepository;
        this.ontologyService = ontologyService;
        this.agentBridge = agentBridge;
    }

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
            sendOntologyUpdate(new OntologyUpdateCommand("IMPORT", rows, null, null, null));
            return importBatchRepository.complete(pending.batchId(), rows.size(), rows.size());
        } catch (RuntimeException ex) {
            return importBatchRepository.fail(pending.batchId(), rows.size(), ex.getMessage());
        }
    }

    public List<PropertyOntologyRecord> listProperties() {
        return ontologyService.findAllPropertyIds().stream()
                .map(ontologyService::findProperty)
                .flatMap(Optional::stream)
                .sorted(java.util.Comparator.comparing(PropertyOntologyRecord::id))
                .toList();
    }

    public Optional<PropertyEditForm> findEditForm(String propertyId) {
        return ontologyService.findProperty(propertyId).map(PropertyEditForm::fromRecord);
    }

    public void updateProperty(PropertyEditForm form) {
        sendOntologyUpdate(new OntologyUpdateCommand("IMPORT", List.of(form.toImportRow()), null, null, null));
    }

    public void markUnavailable(String propertyId) {
        sendOntologyUpdate(new OntologyUpdateCommand("UPDATE_AVAILABILITY", null, propertyId, null, false));
    }

    private void sendOntologyUpdate(OntologyUpdateCommand command) {
        String requestId = UUID.randomUUID().toString();
        AgentMessage<OntologyUpdateCommand> request = new AgentMessage<>(requestId, "UPDATE_ONTOLOGY", command);
        CompletableFuture<AgentMessage<?>> future = new CompletableFuture<>();
        agentBridge.registerRequest(requestId, future);
        agentBridge.sendRequest(request);
        try {
            future.get(AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new RuntimeException("Ontology update failed: " + ex.getMessage(), ex);
        }
    }
}
