package com.makemytour.entity;

import com.makemytour.enums.StoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a user-generated travel story (blog-style post).
 * Goes through a moderation pipeline before becoming publicly visible.
 *
 * Feature 4 – User-Generated Content & Community.
 */
@Entity
@Table(name = "travel_stories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Rich-text content stored as HTML string.
     * The React frontend uses a rich-text editor (e.g., react-quill equivalent)
     * to produce sanitised HTML that is stored here verbatim.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Cover image URL (optional) */
    private String coverImageUrl;

    /** Destination the story is about (e.g., "Goa", "Manali") */
    private String destination;

    /**
     * Moderation status.
     * New stories start as PENDING; admins approve or reject.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StoryStatus status = StoryStatus.PENDING;

    /** Reason supplied by the moderator when rejecting a story */
    private String rejectionReason;

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
