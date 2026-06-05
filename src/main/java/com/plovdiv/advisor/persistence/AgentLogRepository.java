package com.plovdiv.advisor.persistence;

import com.plovdiv.advisor.dto.AgentLogEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AgentLogRepository extends JpaRepository<AgentLogEntity, Long> {

    List<AgentLogEntity> findByOrderByCreatedAtDescIdDesc(PageRequest pageRequest);

    List<AgentLogEntity> findEntitiesByRequestIdOrderByCreatedAtAscIdAsc(String requestId);

    default void save(String requestId, String sender, String receiver, String performative, String messageSummary) {
        AgentLogEntity entity = new AgentLogEntity();
        entity.setRequestId(requestId);
        entity.setSender(sender);
        entity.setReceiver(receiver);
        entity.setPerformative(performative);
        entity.setMessageSummary(messageSummary);
        save(entity);
    }

    default List<AgentLogEntry> findRecent(int limit) {
        return findByOrderByCreatedAtDescIdDesc(PageRequest.of(0, Math.max(1, Math.min(500, limit)))).stream()
                .map(AgentLogRepository::toEntry)
                .toList();
    }

    default List<AgentLogEntry> findByRequestId(String requestId) {
        return findEntitiesByRequestIdOrderByCreatedAtAscIdAsc(requestId).stream()
                .map(AgentLogRepository::toEntry)
                .toList();
    }

    private static AgentLogEntry toEntry(AgentLogEntity entity) {
        return new AgentLogEntry(
                entity.getId(),
                entity.getRequestId(),
                entity.getSender(),
                entity.getReceiver(),
                entity.getPerformative(),
                entity.getMessageSummary(),
                Instant.parse(entity.getCreatedAt())
        );
    }
}
