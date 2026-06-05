package com.plovdiv.advisor.ontology;

import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.dto.PropertyType;
import com.plovdiv.advisor.service.PropertyCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OntologyServiceTests {

    @TempDir
    private Path tempDir;

    private OntologyService ontologyService;
    private PropertyImportRow apartment;
    private PropertyImportRow house;

    @BeforeEach
    void setUp() throws Exception {
        Path ontologyCopy = tempDir.resolve("plovdiv-real-estate.owl");
        Files.copy(new ClassPathResource("ontology/plovdiv-real-estate.owl").getInputStream(), ontologyCopy);
        ontologyService = new OntologyService(ontologyCopy);

        List<PropertyImportRow> rows = new PropertyCsvParser().parse(new ClassPathResource("data/properties-plovdiv.csv"));
        apartment = rows.stream().filter(row -> row.type() == PropertyType.APARTMENT).findFirst().orElseThrow();
        house = rows.stream().filter(row -> row.type() == PropertyType.HOUSE).findFirst().orElseThrow();
    }

    @Test
    void upsertsApartmentAndHouseIndividualsWithExpectedFacts() {
        ontologyService.upsertProperty(apartment);
        ontologyService.upsertProperty(house);

        PropertyOntologyRecord apartmentRecord = ontologyService.findProperty(apartment.id()).orElseThrow();
        PropertyOntologyRecord houseRecord = ontologyService.findProperty(house.id()).orElseThrow();

        assertThat(apartmentRecord.type()).isEqualTo(PropertyType.APARTMENT);
        assertThat(apartmentRecord.title()).isEqualTo(apartment.title());
        assertThat(apartmentRecord.priceEUR()).isEqualByComparingTo(apartment.priceEUR());
        assertThat(apartmentRecord.pricePerSqM()).isEqualByComparingTo(apartment.pricePerSqM());
        assertThat(apartmentRecord.district()).isEqualTo(apartment.district());
        assertThat(apartmentRecord.hasBalcony()).isEqualTo(apartment.hasBalcony());
        assertThat(houseRecord.type()).isEqualTo(PropertyType.HOUSE);
    }

    @Test
    void updatesPriceAvailabilityAndPersistsChanges() {
        ontologyService.upsertProperty(apartment);
        ontologyService.updatePrice(apartment.id(), new BigDecimal("100000"));
        ontologyService.updateAvailability(apartment.id(), false);
        ontologyService.save();
        ontologyService.load();

        PropertyOntologyRecord record = ontologyService.findProperty(apartment.id()).orElseThrow();

        assertThat(record.priceEUR()).isEqualByComparingTo("100000");
        assertThat(record.pricePerSqM()).isEqualByComparingTo(new BigDecimal("1785.71"));
        assertThat(record.available()).isFalse();
    }

    @Test
    void recalculatesSuitabilityRelationsDuringUpsert() {
        ontologyService.upsertProperty(apartment);

        PropertyOntologyRecord record = ontologyService.findProperty(apartment.id()).orElseThrow();

        assertThat(record.suitableProfiles())
                .contains("FamilyProfile", "StudentProfile", "InvestorProfile", "YoungProfessionalProfile");
    }
}
