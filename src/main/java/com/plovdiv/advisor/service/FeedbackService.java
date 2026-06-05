package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.FeedbackEntry;
import com.plovdiv.advisor.persistence.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public void save(String propertyId, int rating, String comment, boolean useful) {
        int normalizedRating = Math.max(1, Math.min(5, rating));
        String normalizedComment = comment == null ? "" : comment.trim();
        feedbackRepository.save(propertyId, normalizedRating, normalizedComment, useful);
    }

    public List<FeedbackEntry> findByPropertyId(String propertyId) {
        return feedbackRepository.findByPropertyId(propertyId);
    }
}
