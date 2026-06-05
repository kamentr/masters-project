package com.plovdiv.advisor.service;

import com.plovdiv.advisor.persistence.FeedbackRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeedbackServiceTests {

    @Test
    void normalizesRatingAndCommentBeforeSaving() {
        FeedbackRepository repository = mock(FeedbackRepository.class);
        FeedbackService service = new FeedbackService(repository);

        service.save("P001", 9, "  Good match  ", true);

        verify(repository).save("P001", 5, "Good match", true);
    }
}
