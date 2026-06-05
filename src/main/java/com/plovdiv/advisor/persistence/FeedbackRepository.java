package com.plovdiv.advisor.persistence;

import com.plovdiv.advisor.dto.FeedbackEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    List<FeedbackEntity> findEntitiesByPropertyIdOrderByCreatedAtDesc(String propertyId);

    default void save(String propertyId, int rating, String comment, boolean useful) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.setPropertyId(propertyId);
        entity.setRating(rating);
        entity.setComment(comment);
        entity.setUseful(useful ? 1 : 0);
        save(entity);
    }

    default List<FeedbackEntry> findByPropertyId(String propertyId) {
        return findEntitiesByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
                .map(FeedbackRepository::toEntry)
                .toList();
    }

    private static FeedbackEntry toEntry(FeedbackEntity entity) {
        return new FeedbackEntry(
                entity.getId(),
                entity.getUserId(),
                entity.getPropertyId(),
                entity.getRating(),
                entity.getComment(),
                entity.getUseful() == 1,
                Instant.parse(entity.getCreatedAt())
        );
    }
}
