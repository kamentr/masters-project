package com.plovdiv.advisor.dto;

import java.math.BigDecimal;

public record MapMarker(
        String propertyId,
        String title,
        String district,
        BigDecimal latitude,
        BigDecimal longitude,
        int score
) {
}
