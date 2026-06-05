package com.plovdiv.advisor.dto;

import java.time.Instant;

public record AgentLogEntry(
        Long id,
        String requestId,
        String sender,
        String receiver,
        String performative,
        String messageSummary,
        Instant createdAt
) {
}
