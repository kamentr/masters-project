package com.plovdiv.advisor.dto;

import java.math.BigDecimal;
import java.util.List;

public record OntologyUpdateCommand(
        String action,
        List<PropertyImportRow> properties,
        String propertyId,
        BigDecimal priceEUR,
        Boolean available
) {
}
