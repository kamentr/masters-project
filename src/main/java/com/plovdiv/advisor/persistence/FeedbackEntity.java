package com.plovdiv.advisor.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "feedback")
@Getter
@Setter
class FeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(nullable = false)
    private int rating;

    private String comment;

    @Column(nullable = false)
    private int useful;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now().toString();
        }
    }
}
