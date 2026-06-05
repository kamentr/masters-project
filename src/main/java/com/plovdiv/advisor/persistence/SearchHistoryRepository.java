package com.plovdiv.advisor.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plovdiv.advisor.dto.SearchCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Long> {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    Optional<SearchHistoryEntity> findFirstByOrderByIdDesc();

    default void save(SearchCriteria criteria) {
        try {
            SearchHistoryEntity entity = new SearchHistoryEntity();
            entity.setCriteriaJson(OBJECT_MAPPER.writeValueAsString(criteria));
            entity.setSelectedProfile(criteria.profile() == null ? "" : criteria.profile().name());
            save(entity);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize search criteria", ex);
        }
    }

    default Optional<SearchCriteria> findLatest() {
        return findFirstByOrderByIdDesc()
                .flatMap(SearchHistoryRepository::toCriteria);
    }

    private static Optional<SearchCriteria> toCriteria(SearchHistoryEntity entity) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(entity.getCriteriaJson(), SearchCriteria.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
