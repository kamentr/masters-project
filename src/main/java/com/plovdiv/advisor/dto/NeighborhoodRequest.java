package com.plovdiv.advisor.dto;

import java.util.List;

public record NeighborhoodRequest(
        List<String> propertyIds,
        BuyerProfile profile,
        List<String> priorities
) {
}
