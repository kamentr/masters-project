package com.plovdiv.advisor.service;

import com.plovdiv.advisor.agent.AgentBridge;
import com.plovdiv.advisor.dto.AgentMessage;
import com.plovdiv.advisor.dto.ImportBatchResult;
import com.plovdiv.advisor.dto.OntologyUpdateCommand;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.persistence.ImportBatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyImportServiceTests {

    private final PropertyCsvParser csvParser = new PropertyCsvParser();
    private final ImportBatchRepository importBatchRepository = mock(ImportBatchRepository.class);
    private final OntologyService ontologyService = mock(OntologyService.class);
    private final AgentBridge agentBridge = mock(AgentBridge.class);
    private final PropertyImportService service = new PropertyImportService(
            csvParser,
            importBatchRepository,
            ontologyService,
            agentBridge
    );

    @Test
    void importsValidCsvThroughOntologyUpdateAgent() {
        String csv = """
                id,title,type,district,priceEUR,areaSqM,rooms,bedrooms,floor,totalFloors,constructionType,yearBuilt,heatingType,hasElevator,hasParking,hasBalcony,latitude,longitude,isAvailable,distanceToSchool,distanceToKindergarten,distanceToUniversity,distanceToPark,distanceToTransport,distanceToHospital,distanceToPharmacy
                P900,Test apartment,Apartment,Center,100000,80,3,2,2,6,Brick,2020,Electric,true,false,true,42.14,24.75,true,300,300,400,500,200,1000,300
                """;
        MockMultipartFile file = new MockMultipartFile("file", "properties.csv", "text/csv", csv.getBytes());
        when(importBatchRepository.create("properties.csv"))
                .thenReturn(new ImportBatchResult(10, "PENDING", 0, 0, 0, null));
        when(importBatchRepository.complete(10, 1, 1))
                .thenReturn(new ImportBatchResult(10, "SUCCESS", 1, 1, 0, null));

        AtomicReference<CompletableFuture<AgentMessage<?>>> futureRef = new AtomicReference<>();
        doAnswer(invocation -> {
            futureRef.set(invocation.getArgument(1));
            return null;
        }).when(agentBridge).registerRequest(any(), any());
        doAnswer(invocation -> {
            AgentMessage<OntologyUpdateCommand> request = invocation.getArgument(0);
            futureRef.get().complete(new AgentMessage<>(request.getRequestId(), "UPDATE_ONTOLOGY_SUCCESS", "ok"));
            return null;
        }).when(agentBridge).sendRequest(any());

        ImportBatchResult result = service.importCsv(file);

        assertThat(result.successful()).isTrue();
        verify(importBatchRepository).complete(10, 1, 1);
        verify(agentBridge).sendRequest(any());
    }

    @Test
    void recordsFailedBatchForInvalidCsv() {
        String csv = """
                id,title,type,district,priceEUR,areaSqM,rooms,bedrooms,floor,totalFloors,constructionType,yearBuilt,heatingType,hasElevator,hasParking,hasBalcony,latitude,longitude,isAvailable,distanceToKindergarten,distanceToUniversity,distanceToPark,distanceToTransport,distanceToHospital,distanceToPharmacy
                P900,Invalid,Apartment,Center,100000,80,3,2,2,6,Brick,2020,Electric,true,false,true,42.14,24.75,true,300,400,500,200,1000,300
                """;
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", csv.getBytes());
        when(importBatchRepository.create("bad.csv"))
                .thenReturn(new ImportBatchResult(11, "PENDING", 0, 0, 0, null));
        when(importBatchRepository.fail(eq(11L), eq(0), any()))
                .thenReturn(new ImportBatchResult(11, "FAILED", 0, 0, 0, "Missing required CSV column 'distanceToSchool'"));

        ImportBatchResult result = service.importCsv(file);

        assertThat(result.successful()).isFalse();
        verify(importBatchRepository).fail(eq(11L), eq(0), any());
    }
}
