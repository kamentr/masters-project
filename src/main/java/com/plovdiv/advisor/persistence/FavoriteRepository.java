package com.plovdiv.advisor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    long DEMO_USER_ID = 1L;

    boolean existsByUserIdAndPropertyId(Long userId, String propertyId);

    List<FavoriteEntity> findByUserIdOrderByIdAsc(Long userId);

    @Transactional
    void deleteByUserIdAndPropertyId(Long userId, String propertyId);

    @Transactional
    default void addFavorite(String propertyId) {
        if (!existsByUserIdAndPropertyId(DEMO_USER_ID, propertyId)) {
            FavoriteEntity entity = new FavoriteEntity();
            entity.setUserId(DEMO_USER_ID);
            entity.setPropertyId(propertyId);
            save(entity);
        }
    }

    @Transactional
    default void removeFavorite(String propertyId) {
        deleteByUserIdAndPropertyId(DEMO_USER_ID, propertyId);
    }

    default List<String> getFavoritePropertyIds() {
        return findByUserIdOrderByIdAsc(DEMO_USER_ID).stream()
                .map(FavoriteEntity::getPropertyId)
                .toList();
    }

    default boolean isFavorite(String propertyId) {
        return existsByUserIdAndPropertyId(DEMO_USER_ID, propertyId);
    }
}
