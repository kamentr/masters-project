package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.dto.PropertyType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyCsvParserTests {

    private final PropertyCsvParser parser = new PropertyCsvParser();

    @Test
    void parsesSeedCsvAndCalculatesPricePerSquareMeter() {
        List<PropertyImportRow> rows = parser.parse(new ClassPathResource("data/properties-plovdiv.csv"));

        assertThat(rows).hasSize(72);
        assertThat(rows.getFirst().id()).isEqualTo("P001");
        assertThat(rows.getFirst().type()).isEqualTo(PropertyType.APARTMENT);
        assertThat(rows.getFirst().district()).isEqualTo(District.CENTER);
        assertThat(rows.getFirst().pricePerSqM()).isEqualByComparingTo(new BigDecimal("1479.86"));
    }

    @Test
    void rejectsMissingRequiredAmenityDistanceColumn() {
        String csv = """
                id,title,type,district,priceEUR,areaSqM,rooms,bedrooms,floor,totalFloors,constructionType,yearBuilt,heatingType,hasElevator,hasParking,hasBalcony,latitude,longitude,isAvailable,distanceToKindergarten,distanceToUniversity,distanceToPark,distanceToTransport,distanceToHospital,distanceToPharmacy
                P999,Invalid,Apartment,Center,100000,80,3,2,2,6,Brick,2020,Electric,true,false,true,42.14,24.75,true,300,400,500,200,1000,300
                """;

        assertThatThrownBy(() -> parser.parse(new StringReader(csv)))
                .isInstanceOf(CsvImportValidationException.class)
                .hasMessageContaining("Missing required CSV column 'distanceToSchool'");
    }

    @Test
    void rejectsInvalidRowValuesWithHelpfulErrors() {
        String csv = String.join("\n",
                String.join(",", PropertyCsvParser.REQUIRED_HEADERS),
                "P998,Bad,Apartment,Center,-1,80,3,2,9,6,Brick,2020,Electric,yes,false,true,42.14,24.75,true,300,300,400,500,200,1000,300"
        );

        assertThatThrownBy(() -> parser.parse(new StringReader(csv)))
                .isInstanceOf(CsvImportValidationException.class)
                .hasMessageContaining("priceEUR must be greater than 0");
    }
}
