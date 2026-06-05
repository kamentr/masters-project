package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.persistence.FavoriteRepository;
import com.plovdiv.advisor.persistence.SearchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CompareAndFavoritesTests {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.execute("DELETE FROM favorites");
        jdbcTemplate.execute("DELETE FROM search_history");
    }

    @Test
    void testFavoritesFlow() {
        assertThat(favoriteRepository.getFavoritePropertyIds()).isEmpty();
        assertThat(favoriteRepository.isFavorite("P001")).isFalse();

        favoriteRepository.addFavorite("P001");
        assertThat(favoriteRepository.getFavoritePropertyIds()).containsExactly("P001");
        assertThat(favoriteRepository.isFavorite("P001")).isTrue();

        favoriteRepository.addFavorite("P001");
        assertThat(favoriteRepository.getFavoritePropertyIds()).containsExactly("P001");

        favoriteRepository.addFavorite("P002");
        assertThat(favoriteRepository.getFavoritePropertyIds()).contains("P001", "P002");

        favoriteRepository.removeFavorite("P001");
        assertThat(favoriteRepository.getFavoritePropertyIds()).containsExactly("P002");
        assertThat(favoriteRepository.isFavorite("P001")).isFalse();
    }

    @Test
    void testSearchHistoryLatest() {
        assertThat(searchHistoryRepository.findLatest()).isEmpty();

        SearchCriteria criteria1 = new SearchCriteria(
                BuyerProfile.STUDENT,
                new BigDecimal("120000"),
                Collections.emptyList(),
                1,
                1,
                null,
                null,
                false,
                false,
                false,
                Collections.emptyList()
        );

        searchHistoryRepository.save(criteria1);
        Optional<SearchCriteria> latest = searchHistoryRepository.findLatest();
        assertThat(latest).isPresent();
        assertThat(latest.get().profile()).isEqualTo(BuyerProfile.STUDENT);
        assertThat(latest.get().maxBudgetEUR()).isEqualByComparingTo("120000");

        SearchCriteria criteria2 = new SearchCriteria(
                BuyerProfile.FAMILY,
                new BigDecimal("220000"),
                Collections.emptyList(),
                2,
                2,
                null,
                null,
                false,
                false,
                false,
                Collections.emptyList()
        );

        searchHistoryRepository.save(criteria2);
        latest = searchHistoryRepository.findLatest();
        assertThat(latest).isPresent();
        assertThat(latest.get().profile()).isEqualTo(BuyerProfile.FAMILY);
        assertThat(latest.get().maxBudgetEUR()).isEqualByComparingTo("220000");
    }
}
