package com.makemytour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Multi-dimensional review entity for the Rate & Review System (RRS).
 *
 * Scores three independent dimensions:
 *   - Punctuality (1–5): Was the service on time?
 *   - Cleanliness  (1–5): Was the vehicle/room clean?
 *   - Amenities    (1–5): Were facilities/features satisfactory?
 *
 * A weighted average with time-decay is calculated by the
 * DetailedReviewRepository JPQL query to produce the composite score
 * that appears on listings.
 *
 * Feature 6 – Rate & Review System (RRS).
 */
@Entity
@Table(name = "detailed_reviews",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "booking_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Punctuality score: 1 (very late) – 5 (perfectly on time) */
    @Column(nullable = false)
    private int punctualityScore;

    /** Cleanliness score: 1 (dirty) – 5 (spotless) */
    @Column(nullable = false)
    private int cleanlinessScore;

    /** Amenities score: 1 (poor) – 5 (excellent) */
    @Column(nullable = false)
    private int amenitiesScore;

    /** Optional written comment */
    @Column(length = 2000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
