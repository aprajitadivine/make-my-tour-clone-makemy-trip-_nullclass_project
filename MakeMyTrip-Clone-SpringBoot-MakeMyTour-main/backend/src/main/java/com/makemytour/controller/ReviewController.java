package com.makemytour.controller;

import com.makemytour.dto.ReviewRequest;
import com.makemytour.entity.Review;
import com.makemytour.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the Review system (Feature 2).
 * Supports 1–5 star ratings with optional image URL.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** POST /api/reviews – Submits a new review for a booking */
    @PostMapping
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        try {
            Review review = reviewService.createReview(userDetails.getUsername(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/reviews/flight/{flightId} – Lists all reviews for a flight.
     *  Optional ?sort=helpful to sort by most helpful first (Feature 2). */
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<Review>> getFlightReviews(
            @PathVariable Long flightId,
            @RequestParam(defaultValue = "false") boolean sort) {
        return ResponseEntity.ok(reviewService.getReviewsByFlight(flightId, sort));
    }

    /** GET /api/reviews/hotel/{hotelId} – Lists all reviews for a hotel.
     *  Optional ?sort=true to sort by most helpful first (Feature 2). */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<Review>> getHotelReviews(
            @PathVariable Long hotelId,
            @RequestParam(defaultValue = "false") boolean sort) {
        return ResponseEntity.ok(reviewService.getReviewsByHotel(hotelId, sort));
    }

    /** GET /api/reviews/my – Lists all reviews written by the current user */
    @GetMapping("/my")
    public ResponseEntity<List<Review>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reviewService.getReviewsByUsername(userDetails.getUsername()));
    }

    /** GET /api/reviews/flight/{flightId}/average – Average rating for a flight */
    @GetMapping("/flight/{flightId}/average")
    public ResponseEntity<Map<String, Object>> getFlightAverageRating(@PathVariable Long flightId) {
        Double avg = reviewService.getAverageFlightRating(flightId);
        return ResponseEntity.ok(Map.of(
            "flightId", flightId,
            "averageRating", avg != null ? avg : 0.0
        ));
    }

    /** GET /api/reviews/hotel/{hotelId}/average – Average rating for a hotel */
    @GetMapping("/hotel/{hotelId}/average")
    public ResponseEntity<Map<String, Object>> getHotelAverageRating(@PathVariable Long hotelId) {
        Double avg = reviewService.getAverageHotelRating(hotelId);
        return ResponseEntity.ok(Map.of(
            "hotelId", hotelId,
            "averageRating", avg != null ? avg : 0.0
        ));
    }

    /** PUT /api/reviews/{id} – Updates an existing review */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        try {
            Review review = reviewService.updateReview(userDetails.getUsername(), id, request);
            return ResponseEntity.ok(review);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/reviews/{id} – Deletes a review */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            reviewService.deleteReview(userDetails.getUsername(), id);
            return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/reviews/{id}/helpful – Marks a review as helpful (Feature 2).
     * Increments the helpfulVotes counter so reviews can be sorted by usefulness.
     */
    @PostMapping("/{id}/helpful")
    public ResponseEntity<?> markHelpful(@PathVariable Long id) {
        try {
            Review review = reviewService.markHelpful(id);
            return ResponseEntity.ok(Map.of(
                "reviewId", id,
                "helpfulVotes", review.getHelpfulVotes(),
                "message", "Marked as helpful"
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/reviews/{id}/flag – Flags a review as inappropriate (Feature 2).
     * Flagged reviews are hidden from public view pending moderation.
     */
    @PostMapping("/{id}/flag")
    public ResponseEntity<?> flagReview(@PathVariable Long id) {
        try {
            reviewService.flagReview(id);
            return ResponseEntity.ok(Map.of(
                "reviewId", id,
                "message", "Review flagged for moderation. Thank you for keeping our community safe."
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/reviews/flagged – Lists all flagged reviews for admin moderation.
     */
    @GetMapping("/flagged")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Review>> getFlaggedReviews() {
        return ResponseEntity.ok(reviewService.getFlaggedReviews());
    }

    /**
     * POST /api/reviews/{id}/unflag – Restores a flagged review to public visibility (admin only).
     */
    @PostMapping("/{id}/unflag")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unflagReview(@PathVariable Long id) {
        try {
            Review review = reviewService.unflagReview(id);
            return ResponseEntity.ok(Map.of("reviewId", id, "message", "Review restored to public feed"));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
