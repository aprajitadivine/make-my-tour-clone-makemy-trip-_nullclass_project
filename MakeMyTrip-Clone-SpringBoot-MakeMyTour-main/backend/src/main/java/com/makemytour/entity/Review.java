package com.makemytour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Review entity supporting the 1–5 star rating system with optional image URL.
 * Users can review any travel product after completing a booking.
 */
@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "booking_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Star rating: must be between 1 and 5 (inclusive) */
    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String comment;

    /** Optional image URL (screenshot, photo of the experience, etc.) */
    private String imageUrl;

    /**
     * Number of users who marked this review as "helpful".
     * Used for sorting by most helpful.
     */
    @Column(nullable = false)
    @Builder.Default
    private int helpfulVotes = 0;

    /**
     * Whether this review has been flagged as inappropriate by another user.
     * Flagged reviews are hidden pending moderation.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean flagged = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
