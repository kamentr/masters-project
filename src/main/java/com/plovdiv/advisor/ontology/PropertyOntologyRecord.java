package com.plovdiv.advisor.ontology;

import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.PropertyType;

import java.math.BigDecimal;
import java.util.Set;

public record PropertyOntologyRecord(
        String id,
        String title,
        PropertyType type,
        District district,
        BigDecimal priceEUR,
        BigDecimal areaSqM,
        BigDecimal pricePerSqM,
        int rooms,
        int bedrooms,
        int floor,
        int totalFloors,
        ConstructionType constructionType,
        int yearBuilt,
        HeatingType heatingType,
        boolean available,
        boolean hasElevator,
        boolean hasParking,
        boolean hasBalcony,
        BigDecimal latitude,
        BigDecimal longitude,
        int distanceToSchool,
        int distanceToKindergarten,
        int distanceToUniversity,
        int distanceToPark,
        int distanceToTransport,
        int distanceToHospital,
        int distanceToPharmacy,
        Set<String> suitableProfiles
) {
}
