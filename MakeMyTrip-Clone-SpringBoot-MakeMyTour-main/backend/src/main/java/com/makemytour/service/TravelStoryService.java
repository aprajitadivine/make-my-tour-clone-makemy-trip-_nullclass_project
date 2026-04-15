package com.makemytour.service;

import com.makemytour.dto.TravelStoryRequest;
import com.makemytour.entity.TravelStory;
import com.makemytour.entity.User;
import com.makemytour.enums.StoryStatus;
import com.makemytour.repository.TravelStoryRepository;
import com.makemytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Business logic for the Travel Stories module.
 *
 * Moderation pipeline:
 *   1. User submits story → status = PENDING
 *   2. Admin reviews and calls approve/reject
 *   3. Approved stories appear in the public feed
 *
 * Feature 4 – User-Generated Content & Community.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TravelStoryService {

    private final TravelStoryRepository storyRepository;
    private final UserRepository userRepository;

    /** Submits a new travel story (starts in PENDING status) */
    @Transactional
    public TravelStory createStory(String username, TravelStoryRequest request) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        TravelStory story = TravelStory.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .coverImageUrl(request.getCoverImageUrl())
                .destination(request.getDestination())
                .status(StoryStatus.PENDING)
                .build();

        TravelStory saved = storyRepository.save(story);
        log.info("Travel story '{}' submitted by {} (status: PENDING)", saved.getTitle(), username);
        return saved;
    }

    /** Returns all APPROVED stories for the public feed */
    public List<TravelStory> getApprovedStories() {
        return storyRepository.findByStatusOrderByCreatedAtDesc(StoryStatus.APPROVED);
    }

    /** Returns all stories written by the current user (any status) */
    public List<TravelStory> getMyStories(String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return storyRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId());
    }

    /** Returns all PENDING stories for admin moderation */
    public List<TravelStory> getPendingStories() {
        return storyRepository.findByStatus(StoryStatus.PENDING);
    }

    /** Approves a story – only admins should call this */
    @Transactional
    public TravelStory approveStory(Long storyId) {
        TravelStory story = findById(storyId);
        story.setStatus(StoryStatus.APPROVED);
        log.info("Story id={} approved", storyId);
        return storyRepository.save(story);
    }

    /** Rejects a story with an optional reason – only admins should call this */
    @Transactional
    public TravelStory rejectStory(Long storyId, String reason) {
        TravelStory story = findById(storyId);
        story.setStatus(StoryStatus.REJECTED);
        story.setRejectionReason(reason);
        log.info("Story id={} rejected (reason: {})", storyId, reason);
        return storyRepository.save(story);
    }

    /** Allows the author to delete their own story */
    @Transactional
    public void deleteStory(String username, Long storyId) {
        TravelStory story = findById(storyId);
        if (!story.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only delete your own stories");
        }
        storyRepository.deleteById(storyId);
    }

    private TravelStory findById(Long id) {
        return storyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Travel story not found: " + id));
    }
}
