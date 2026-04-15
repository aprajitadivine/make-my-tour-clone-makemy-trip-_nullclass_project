package com.makemytour.controller;

import com.makemytour.dto.TravelStoryRequest;
import com.makemytour.entity.TravelStory;
import com.makemytour.service.TravelStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API for the Travel Stories module.
 * Feature 4 – User-Generated Content & Community.
 *
 * Moderation pipeline:
 *   POST /api/stories            → submit (PENDING)
 *   POST /api/stories/{id}/approve → admin only (APPROVED)
 *   POST /api/stories/{id}/reject  → admin only (REJECTED)
 */
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class TravelStoryController {

    private final TravelStoryService storyService;

    /** POST /api/stories – Submit a new travel story (starts PENDING) */
    @PostMapping
    public ResponseEntity<?> createStory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TravelStoryRequest request) {
        try {
            TravelStory story = storyService.createStory(userDetails.getUsername(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(story);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/stories – Returns all APPROVED stories (public feed) */
    @GetMapping
    public ResponseEntity<List<TravelStory>> getApprovedStories() {
        return ResponseEntity.ok(storyService.getApprovedStories());
    }

    /** GET /api/stories/my – Returns all of the current user's stories */
    @GetMapping("/my")
    public ResponseEntity<List<TravelStory>> getMyStories(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(storyService.getMyStories(userDetails.getUsername()));
    }

    /** GET /api/stories/pending – Returns stories awaiting moderation (admin only) */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TravelStory>> getPendingStories() {
        return ResponseEntity.ok(storyService.getPendingStories());
    }

    /** POST /api/stories/{id}/approve – Approves a story (admin only) */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveStory(@PathVariable Long id) {
        try {
            TravelStory story = storyService.approveStory(id);
            return ResponseEntity.ok(story);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /api/stories/{id}/reject – Rejects a story (admin only) */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectStory(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            TravelStory story = storyService.rejectStory(id, reason);
            return ResponseEntity.ok(story);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** DELETE /api/stories/{id} – Author deletes their own story */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            storyService.deleteStory(userDetails.getUsername(), id);
            return ResponseEntity.ok(Map.of("message", "Story deleted"));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
