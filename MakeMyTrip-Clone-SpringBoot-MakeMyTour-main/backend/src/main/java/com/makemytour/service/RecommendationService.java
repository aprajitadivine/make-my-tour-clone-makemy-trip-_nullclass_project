package com.makemytour.service;

import com.makemytour.entity.Booking;
import com.makemytour.enums.TravelCategory;
import com.makemytour.repository.BookingRepository;
import com.makemytour.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Feature 6 – AI Recommendation Engine (Mock)
 *
 * Suggests a destination based on the TravelCategory of the user's most recent
 * booking. Returns both the recommendation and a human-readable "Why this?" reason
 * to make the recommendation transparent and trustworthy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Destination recommendations keyed by TravelCategory.
     */
    private static final Map<TravelCategory, String> CATEGORY_MAP = Map.of(
        TravelCategory.BEACH,        "🌊 Goa or Maldives – pristine beaches await you!",
        TravelCategory.HILL_STATION, "🏔️ Shimla or Manali – escape to the cool mountains!",
        TravelCategory.HERITAGE,     "🏛️ Agra or Jaipur – explore India's royal history!",
        TravelCategory.ADVENTURE,    "🧗 Rishikesh or Ladakh – thrill-seeker's paradise!",
        TravelCategory.PILGRIMAGE,   "🙏 Varanasi or Tirupati – a spiritual journey awaits!",
        TravelCategory.CITY_TOUR,    "🏙️ Mumbai or Delhi – vibrant city experiences!",
        TravelCategory.HONEYMOON,    "💑 Kerala or Andaman – the perfect romantic getaway!",
        TravelCategory.WILDLIFE,     "🦁 Jim Corbett or Ranthambore – witness wild India!"
    );

    /**
     * "Why this?" explanations for each category.
     */
    private static final Map<TravelCategory, String> REASON_MAP = Map.of(
        TravelCategory.BEACH,        "Based on your last booking, you enjoy beach destinations. Goa and Maldives are consistently top-rated for beach lovers.",
        TravelCategory.HILL_STATION, "Your travel history shows a preference for hill stations. Shimla and Manali are popular for scenic mountain escapes.",
        TravelCategory.HERITAGE,     "You've booked heritage trips before – Agra and Jaipur offer India's most iconic historical monuments.",
        TravelCategory.ADVENTURE,    "Your bookings suggest you love adventure. Rishikesh and Ladakh offer world-class trekking and rafting.",
        TravelCategory.PILGRIMAGE,   "Based on your pilgrimage bookings, Varanasi and Tirupati are among the most spiritually significant destinations.",
        TravelCategory.CITY_TOUR,    "You enjoy city tours – Mumbai and Delhi offer the best blend of culture, food, and nightlife.",
        TravelCategory.HONEYMOON,    "Your booking category indicates a romantic trip – Kerala's backwaters and Andaman's beaches are perfect.",
        TravelCategory.WILDLIFE,     "You've shown interest in wildlife – Jim Corbett and Ranthambore are India's best tiger reserves."
    );

    /** Shown when the user has no previous bookings */
    private static final String DEFAULT_RECOMMENDATION =
        "✈️ Mumbai – a great first destination! Explore India's city of dreams.";
    private static final String DEFAULT_REASON =
        "This is our top pick for first-time travellers. Book your first trip and we'll personalise your recommendations!";

    /**
     * Immutable result object returned by recommendWithReason().
     */
    @Data
    public static class RecommendationResult {
        private final String recommendation;
        private final String reason;
        private final String category;
    }

    /**
     * Returns a personalised recommendation with its explanation.
     *
     * @param username the authenticated user's username
     * @return RecommendationResult containing text, reason, and category
     */
    public RecommendationResult recommendWithReason(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username))
                .getId();

        Optional<Booking> lastBookingOpt =
            bookingRepository.findTopByUserIdOrderByBookingDateDesc(userId);

        if (lastBookingOpt.isEmpty()) {
            return new RecommendationResult(DEFAULT_RECOMMENDATION, DEFAULT_REASON, "NONE");
        }

        TravelCategory category = lastBookingOpt.get().getCategory();
        if (category == null) {
            return new RecommendationResult(DEFAULT_RECOMMENDATION, DEFAULT_REASON, "NONE");
        }

        String rec = CATEGORY_MAP.getOrDefault(category, DEFAULT_RECOMMENDATION);
        String reason = REASON_MAP.getOrDefault(category, DEFAULT_REASON);
        log.info("Recommendation for user {} (category={}): {}", username, category, rec);
        return new RecommendationResult(rec, reason, category.name());
    }

    /**
     * Convenience method for controllers that only need the recommendation text.
     */
    public String recommendByUsername(String username) {
        return recommendWithReason(username).getRecommendation();
    }

    /**
     * Records thumbs-up / thumbs-down feedback on the recommendation.
     * In production this would write to a feedback table for ML training.
     *
     * @param username the user giving feedback
     * @param helpful  true = helpful (thumbs up), false = not helpful (thumbs down)
     * @param comment  optional free-text note
     */
    public void recordFeedback(String username, boolean helpful, String comment) {
        log.info("Recommendation feedback from {}: helpful={}, comment=\"{}\"",
                username, helpful, comment);
        // TODO: persist to recommendation_feedback table for collaborative filtering
    }

    /**
     * Returns the full recommendation map for use by the admin dashboard.
     */
    public Map<TravelCategory, String> getAllRecommendations() {
        return CATEGORY_MAP;
    }
}
