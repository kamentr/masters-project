package com.plovdiv.advisor.dto;

import java.time.Instant;

public record FeedbackEntry(
        Long id,
        Long userId,
        String propertyId,
        int rating,
        String comment,
        boolean useful,
        Instant createdAt
) {
}
