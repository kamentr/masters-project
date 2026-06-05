package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.dto.PropertyType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PropertyCsvParser {

    public static final List<String> REQUIRED_HEADERS = List.of(
            "id", "title", "type", "district", "priceEUR", "areaSqM", "rooms", "bedrooms", "floor",
            "totalFloors", "constructionType", "yearBuilt", "heatingType", "hasElevator", "hasParking",
            "hasBalcony", "latitude", "longitude", "isAvailable", "distanceToSchool",
            "distanceToKindergarten", "distanceToUniversity", "distanceToPark", "distanceToTransport",
            "distanceToHospital", "distanceToPharmacy"
    );

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("42.00");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("42.30");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("24.50");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("24.90");

    public List<PropertyImportRow> parse(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public List<PropertyImportRow> parse(Reader reader) {
        List<String> errors = new ArrayList<>();
        List<PropertyImportRow> rows = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get()
                .parse(reader)) {
            validateHeaders(parser.getHeaderMap(), errors);
            if (!errors.isEmpty()) {
                throw new CsvImportValidationException(errors);
            }

            Set<String> seenIds = new HashSet<>();
            for (CSVRecord record : parser) {
                try {
                    PropertyImportRow row = parseRecord(record);
                    if (!seenIds.add(row.id())) {
                        errors.add("Row " + record.getRecordNumber() + ": duplicate id '" + row.id() + "'");
                    } else {
                        rows.add(row);
                    }
                } catch (IllegalArgumentException ex) {
                    errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        if (!errors.isEmpty()) {
            throw new CsvImportValidationException(errors);
        }
        return rows;
    }

    private void validateHeaders(Map<String, Integer> headerMap, List<String> errors) {
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!headerMap.containsKey(requiredHeader)) {
                errors.add("Missing required CSV column '" + requiredHeader + "'");
            }
        }
    }

    private PropertyImportRow parseRecord(CSVRecord record) {
        String id = requiredText(record, "id");
        String title = requiredText(record, "title");
        PropertyType type = parseEnum(PropertyType.class, value(record, "type"), "type");
        District district = parseDistrict(value(record, "district"));
        BigDecimal priceEUR = positiveDecimal(record, "priceEUR");
        BigDecimal areaSqM = positiveDecimal(record, "areaSqM");
        int rooms = minInt(record, "rooms", 1);
        int bedrooms = minInt(record, "bedrooms", 0);
        int floor = minInt(record, "floor", 0);
        int totalFloors = minInt(record, "totalFloors", 1);
        if (floor > totalFloors) {
            throw new IllegalArgumentException("floor must be less than or equal to totalFloors");
        }
        ConstructionType constructionType = parseEnum(ConstructionType.class, value(record, "constructionType"), "constructionType");
        int yearBuilt = minInt(record, "yearBuilt", 1850);
        if (yearBuilt > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("yearBuilt cannot be in the far future");
        }
        HeatingType heatingType = parseEnum(HeatingType.class, value(record, "heatingType"), "heatingType");

        return new PropertyImportRow(
                id,
                title,
                type,
                district,
                priceEUR,
                areaSqM,
                rooms,
                bedrooms,
                floor,
                totalFloors,
                constructionType,
                yearBuilt,
                heatingType,
                strictBoolean(record, "hasElevator"),
                strictBoolean(record, "hasParking"),
                strictBoolean(record, "hasBalcony"),
                latitude(record),
                longitude(record),
                strictBoolean(record, "isAvailable"),
                minInt(record, "distanceToSchool", 0),
                minInt(record, "distanceToKindergarten", 0),
                minInt(record, "distanceToUniversity", 0),
                minInt(record, "distanceToPark", 0),
                minInt(record, "distanceToTransport", 0),
                minInt(record, "distanceToHospital", 0),
                minInt(record, "distanceToPharmacy", 0)
        );
    }

    private String value(CSVRecord record, String header) {
        return record.get(header).trim();
    }

    private String requiredText(CSVRecord record, String header) {
        String value = value(record, header);
        if (value.isBlank()) {
            throw new IllegalArgumentException(header + " is required");
        }
        return value;
    }

    private BigDecimal positiveDecimal(CSVRecord record, String header) {
        BigDecimal value = decimal(record, header);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(header + " must be greater than 0");
        }
        return value;
    }

    private BigDecimal decimal(CSVRecord record, String header) {
        try {
            return new BigDecimal(value(record, header));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(header + " must be a decimal number");
        }
    }

    private int minInt(CSVRecord record, String header, int min) {
        try {
            int value = Integer.parseInt(value(record, header));
            if (value < min) {
                throw new IllegalArgumentException(header + " must be at least " + min);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(header + " must be an integer");
        }
    }

    private boolean strictBoolean(CSVRecord record, String header) {
        String value = value(record, header).toLowerCase();
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException(header + " must be true or false");
    }

    private BigDecimal latitude(CSVRecord record) {
        BigDecimal value = decimal(record, "latitude");
        if (value.compareTo(MIN_LATITUDE) < 0 || value.compareTo(MAX_LATITUDE) > 0) {
            throw new IllegalArgumentException("latitude must be a plausible Plovdiv coordinate");
        }
        return value;
    }

    private BigDecimal longitude(CSVRecord record) {
        BigDecimal value = decimal(record, "longitude");
        if (value.compareTo(MIN_LONGITUDE) < 0 || value.compareTo(MAX_LONGITUDE) > 0) {
            throw new IllegalArgumentException("longitude must be a plausible Plovdiv coordinate");
        }
        return value;
    }

    private District parseDistrict(String value) {
        for (District district : District.values()) {
            if (district.csvValue().equalsIgnoreCase(value)) {
                return district;
            }
        }
        throw new IllegalArgumentException("district has unsupported value '" + value + "'");
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String header) {
        String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(header + " has unsupported value '" + value + "'");
        }
    }
}
