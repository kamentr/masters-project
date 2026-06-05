package com.plovdiv.advisor.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PropertyImportRow(
        String id,
        String title,
        PropertyType type,
        District district,
        BigDecimal priceEUR,
        BigDecimal areaSqM,
        int rooms,
        int bedrooms,
        int floor,
        int totalFloors,
        ConstructionType constructionType,
        int yearBuilt,
        HeatingType heatingType,
        boolean hasElevator,
        boolean hasParking,
        boolean hasBalcony,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isAvailable,
        int distanceToSchool,
        int distanceToKindergarten,
        int distanceToUniversity,
        int distanceToPark,
        int distanceToTransport,
        int distanceToHospital,
        int distanceToPharmacy
) {
    public BigDecimal pricePerSqM() {
        return priceEUR.divide(areaSqM, 2, RoundingMode.HALF_UP);
    }
}
