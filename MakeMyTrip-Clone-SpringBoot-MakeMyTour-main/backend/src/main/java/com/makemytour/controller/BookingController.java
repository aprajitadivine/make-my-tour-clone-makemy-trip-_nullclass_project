package com.makemytour.controller;

import com.makemytour.dto.BookingRequest;
import com.makemytour.dto.CancelRequest;
import com.makemytour.entity.Booking;
import com.makemytour.service.BookingService;
import com.makemytour.service.RecommendationService;
import com.makemytour.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API for creating/cancelling bookings and triggering AI recommendations.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final RecommendationService recommendationService;
    private final RefundService refundService;

    /** POST /api/bookings – Creates a new booking (Feature 4 – seatId from SeatMap) */
    @PostMapping
    public ResponseEntity<?> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(userDetails.getUsername(), request);
            return ResponseEntity.ok(booking);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/bookings/my – Returns all bookings for the current user */
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.getMyBookings(userDetails.getUsername()));
    }

    /** GET /api/bookings/{id} – Returns a specific booking */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookingService.getBookingById(userDetails.getUsername(), id));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/bookings/{id}/cancel – Cancels a booking and returns the refund breakdown.
     *
     * Feature 1 – Refund Logic with:
     *  - cancellationReason stored for analytics
     *  - refundPercentage and policy explanation returned
     *  - refundStatus set to PENDING_REFUND
     */
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest cancelRequest) {
        try {
            String reason = cancelRequest != null ? cancelRequest.getReason() : null;
            // cancelBooking returns the updated Booking; totalPrice now holds the refund amount
            Booking booking = bookingService.cancelBooking(userDetails.getUsername(), id, reason);
            double refundAmount = booking.getTotalPrice();

            long hoursUntilDep = Duration.between(LocalDateTime.now(), booking.getTravelDate()).toHours();
            int refundPercent = hoursUntilDep < 0 ? 0 : hoursUntilDep < 24 ? 50 : 100;
            String policy = hoursUntilDep < 0
                ? "No refund – departure has passed"
                : hoursUntilDep < 24
                    ? "50% refund – cancelled within 24 hours of departure"
                    : "Full refund – cancelled more than 24 hours before departure";

            return ResponseEntity.ok(Map.of(
                "message", "Booking cancelled successfully",
                "refundAmount", refundAmount,
                "refundPercent", refundPercent,
                "refundPolicy", policy,
                "refundStatus", "PENDING_REFUND",
                "cancellationReason", reason != null ? reason : ""
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/bookings/recommend – Returns an AI-based destination recommendation (Feature 6).
     * Returns both the recommendation text and the reasoning behind it.
     */
    @GetMapping("/recommend")
    public ResponseEntity<Map<String, String>> getRecommendation(
            @AuthenticationPrincipal UserDetails userDetails) {
        RecommendationService.RecommendationResult result =
            recommendationService.recommendWithReason(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "recommendation", result.getRecommendation(),
            "reason", result.getReason(),
            "category", result.getCategory()
        ));
    }

    /**
     * POST /api/bookings/recommend/feedback – Records user feedback on the recommendation.
     * Feature 6 – feedback loop: thumbs up / down captured for future ML training.
     *
     * Body: { "helpful": true/false, "comment": "optional note" }
     */
    @PostMapping("/recommend/feedback")
    public ResponseEntity<Map<String, String>> submitRecommendationFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> feedback) {
        boolean helpful = Boolean.TRUE.equals(feedback.get("helpful"));
        String comment = feedback.getOrDefault("comment", "").toString();
        recommendationService.recordFeedback(userDetails.getUsername(), helpful, comment);
        return ResponseEntity.ok(Map.of(
            "message", helpful
                ? "Thanks for the positive feedback! We'll keep improving your recommendations."
                : "Thanks for your feedback! We'll refine your recommendations."
        ));
    }
}
