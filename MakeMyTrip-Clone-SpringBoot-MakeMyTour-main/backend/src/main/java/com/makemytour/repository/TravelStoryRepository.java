package com.makemytour.repository;

import com.makemytour.entity.TravelStory;
import com.makemytour.enums.StoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for TravelStory entities.
 * Feature 4 – User-Generated Content & Community.
 */
@Repository
public interface TravelStoryRepository extends JpaRepository<TravelStory, Long> {

    /** Returns all approved stories (public feed) */
    List<TravelStory> findByStatusOrderByCreatedAtDesc(StoryStatus status);

    /** Returns all stories by a specific author */
    List<TravelStory> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /** Returns all stories pending moderation review */
    List<TravelStory> findByStatus(StoryStatus status);
}
