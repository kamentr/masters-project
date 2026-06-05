package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.AgentLogEntry;
import com.plovdiv.advisor.persistence.AgentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentLogService {
    private static final int DEFAULT_LIMIT = 100;

    private final AgentLogRepository agentLogRepository;

    public List<AgentLogEntry> recentLogs(Integer limit) {
        return agentLogRepository.findRecent(limit == null ? DEFAULT_LIMIT : limit);
    }

    public List<AgentLogEntry> logsForRequest(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return List.of();
        }
        return agentLogRepository.findByRequestId(requestId.trim());
    }
}
