package com.makemytour.service;

import com.makemytour.dto.DetailedReviewRequest;
import com.makemytour.entity.Booking;
import com.makemytour.entity.DetailedReview;
import com.makemytour.entity.User;
import com.makemytour.repository.BookingRepository;
import com.makemytour.repository.DetailedReviewRepository;
import com.makemytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Business logic for the multi-dimensional Rate & Review System (RRS).
 *
 * Three scoring dimensions: Punctuality, Cleanliness, Amenities.
 * A weighted average with time-decay is calculated via JPQL in
 * DetailedReviewRepository.
 *
 * Feature 6 – Rate & Review System (RRS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailedReviewService {

    // ── Scoring weights (must sum to 1.0) ─────────────────────────────────
    /** Weight applied to the Punctuality dimension (40%) */
    private static final double WEIGHT_PUNCTUALITY = 0.4;
    /** Weight applied to the Cleanliness dimension (30%) */
    private static final double WEIGHT_CLEANLINESS = 0.3;
    /** Weight applied to the Amenities dimension (30%) */
    private static final double WEIGHT_AMENITIES = 0.3;

    // ── Time-decay parameters ─────────────────────────────────────────────
    /** Reviews submitted within this many days are considered "recent" */
    private static final long DECAY_THRESHOLD_DAYS = 30;
    /** Weight applied to recent reviews (≤ DECAY_THRESHOLD_DAYS) */
    private static final double RECENT_REVIEW_WEIGHT = 1.0;
    /** Discount factor applied to older reviews to favour recent experiences */
    private static final double OLD_REVIEW_WEIGHT = 0.5;

    private final DetailedReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Submits a new multi-dimensional review.
     * The user must own the booking being reviewed.
     */
    @Transactional
    public DetailedReview createReview(String username, DetailedReviewRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Booking not found: " + request.getBookingId()));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only review your own bookings");
        }

        DetailedReview review = DetailedReview.builder()
                .user(user)
                .booking(booking)
                .punctualityScore(request.getPunctualityScore())
                .cleanlinessScore(request.getCleanlinessScore())
                .amenitiesScore(request.getAmenitiesScore())
                .comment(request.getComment())
                .build();

        DetailedReview saved = reviewRepository.save(review);
        log.info("DetailedReview id={} submitted by user {} for booking {}",
                saved.getId(), username, booking.getId());
        return saved;
    }

    /** Returns all detailed reviews for a flight */
    public List<DetailedReview> getFlightReviews(Long flightId) {
        return reviewRepository.findByFlightId(flightId);
    }

    /** Returns all detailed reviews for a hotel */
    public List<DetailedReview> getHotelReviews(Long hotelId) {
        return reviewRepository.findByHotelId(hotelId);
    }

    /**
     * Returns the weighted average composite score for a flight with time-decay.
     *
     * Weights:    Punctuality=40%, Cleanliness=30%, Amenities=30%.
     * Time-decay: reviews ≤ 30 days old carry full weight (1.0);
     *             older reviews are discounted by 50% (0.5) to favour recent experiences.
     */
    public Map<String, Object> getFlightScore(Long flightId) {
        List<DetailedReview> reviews = reviewRepository.findByFlightId(flightId);
        double score = computeWeightedScore(reviews);
        return Map.of(
                "flightId", flightId,
                "weightedAverageScore", score,
                "reviewCount", reviews.size()
        );
    }

    /**
     * Returns the weighted average composite score for a hotel with time-decay.
     */
    public Map<String, Object> getHotelScore(Long hotelId) {
        List<DetailedReview> reviews = reviewRepository.findByHotelId(hotelId);
        double score = computeWeightedScore(reviews);
        return Map.of(
                "hotelId", hotelId,
                "weightedAverageScore", score,
                "reviewCount", reviews.size()
        );
    }

    /**
     * Calculates the weighted average score with time-decay for a list of reviews.
     *
     * Formula per review:
     *   rawScore    = 0.4 * punctuality + 0.3 * cleanliness + 0.3 * amenities
     *   decayFactor = 1.0 if review is ≤ 30 days old, otherwise 0.5
     *   contribution = rawScore * decayFactor
     *
     * Aggregate: SUM(contribution) / SUM(decayFactor)
     *
     * @return rounded weighted average (2 decimal places), or 0.0 if no reviews
     */
    private double computeWeightedScore(List<DetailedReview> reviews) {
        if (reviews.isEmpty()) return 0.0;
        LocalDateTime cutoff = LocalDateTime.now().minus(DECAY_THRESHOLD_DAYS, ChronoUnit.DAYS);
        double weightedSum = 0.0;
        double decaySum = 0.0;
        for (DetailedReview r : reviews) {
            double raw = WEIGHT_PUNCTUALITY * r.getPunctualityScore()
                    + WEIGHT_CLEANLINESS * r.getCleanlinessScore()
                    + WEIGHT_AMENITIES * r.getAmenitiesScore();
            double decay = r.getCreatedAt() != null && r.getCreatedAt().isAfter(cutoff)
                    ? RECENT_REVIEW_WEIGHT
                    : OLD_REVIEW_WEIGHT;
            weightedSum += raw * decay;
            decaySum += decay;
        }
        if (decaySum == 0) return 0.0;
        return Math.round((weightedSum / decaySum) * 100.0) / 100.0;
    }

    /** Returns all reviews by the current user */
    public List<DetailedReview> getMyReviews(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return reviewRepository.findByUserId(user.getId());
    }
}
