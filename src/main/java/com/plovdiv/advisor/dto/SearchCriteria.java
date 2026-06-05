package com.plovdiv.advisor.dto;

import java.math.BigDecimal;
import java.util.List;

public record SearchCriteria(
        BuyerProfile profile,
        BigDecimal maxBudgetEUR,
        List<District> districts,
        int minRooms,
        int minBedrooms,
        ConstructionType constructionType,
        HeatingType heatingType,
        boolean requiresElevator,
        boolean requiresParking,
        boolean requiresBalcony,
        List<String> priorities
) {
}
