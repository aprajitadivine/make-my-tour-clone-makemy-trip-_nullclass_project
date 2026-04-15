package com.makemytour.controller;

import com.makemytour.dto.DetailedReviewRequest;
import com.makemytour.entity.DetailedReview;
import com.makemytour.service.DetailedReviewService;
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
 * REST API for the multi-dimensional Rate & Review System (RRS).
 * Feature 6 – Rate & Review System (RRS).
 *
 * Scoring dimensions: Punctuality, Cleanliness, Amenities.
 * Aggregate endpoint returns weighted average with time-decay.
 */
@RestController
@RequestMapping("/api/detailed-reviews")
@RequiredArgsConstructor
public class DetailedReviewController {

    private final DetailedReviewService reviewService;

    /** POST /api/detailed-reviews – Submits a multi-dimensional review */
    @PostMapping
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DetailedReviewRequest request) {
        try {
            DetailedReview review = reviewService.createReview(userDetails.getUsername(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/detailed-reviews/flight/{flightId} – All detailed reviews for a flight */
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<DetailedReview>> getFlightReviews(@PathVariable Long flightId) {
        return ResponseEntity.ok(reviewService.getFlightReviews(flightId));
    }

    /** GET /api/detailed-reviews/hotel/{hotelId} – All detailed reviews for a hotel */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<DetailedReview>> getHotelReviews(@PathVariable Long hotelId) {
        return ResponseEntity.ok(reviewService.getHotelReviews(hotelId));
    }

    /**
     * GET /api/detailed-reviews/flight/{flightId}/score
     * Returns the weighted average score (Punctuality=40%, Cleanliness=30%, Amenities=30%)
     * with a 50% time-decay discount for reviews older than 30 days.
     */
    @GetMapping("/flight/{flightId}/score")
    public ResponseEntity<Map<String, Object>> getFlightScore(@PathVariable Long flightId) {
        return ResponseEntity.ok(reviewService.getFlightScore(flightId));
    }

    /**
     * GET /api/detailed-reviews/hotel/{hotelId}/score
     * Returns the weighted average score with time-decay for a hotel.
     */
    @GetMapping("/hotel/{hotelId}/score")
    public ResponseEntity<Map<String, Object>> getHotelScore(@PathVariable Long hotelId) {
        return ResponseEntity.ok(reviewService.getHotelScore(hotelId));
    }

    /** GET /api/detailed-reviews/my – Returns all detailed reviews by the current user */
    @GetMapping("/my")
    public ResponseEntity<List<DetailedReview>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reviewService.getMyReviews(userDetails.getUsername()));
    }
}
