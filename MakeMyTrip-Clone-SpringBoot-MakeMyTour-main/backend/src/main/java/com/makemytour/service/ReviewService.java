package com.makemytour.service;

import com.makemytour.entity.Booking;
import com.makemytour.entity.Review;
import com.makemytour.entity.User;
import com.makemytour.repository.BookingRepository;
import com.makemytour.repository.ReviewRepository;
import com.makemytour.repository.UserRepository;
import com.makemytour.dto.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Feature 2 – Review System
 *
 * Provides CRUD operations for 1–5 star reviews with optional image URL support.
 * Users can only review bookings they own, and only once per booking.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new review for a completed booking.
     *
     * @param username the authenticated user's username
     * @param request  the review data (rating 1–5, comment, optional imageUrl)
     * @return the persisted Review entity
     */
    @Transactional
    public Review createReview(String username, ReviewRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Booking not found: " + request.getBookingId()));

        // Ensure the booking belongs to the requesting user
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only review your own bookings");
        }

        // Validate star rating range (1–5)
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review review = Review.builder()
                .user(user)
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrl(request.getImageUrl())
                .build();

        return reviewRepository.save(review);
    }

    /** Returns all reviews for a given flight, optionally sorted by helpful votes */
    public List<Review> getReviewsByFlight(Long flightId, boolean sortByHelpful) {
        return sortByHelpful
            ? reviewRepository.findUnflaggedByBookingFlightIdSortedByHelpful(flightId)
            : reviewRepository.findUnflaggedByBookingFlightId(flightId);
    }

    /** Returns all reviews for a given flight */
    public List<Review> getReviewsByFlight(Long flightId) {
        return reviewRepository.findUnflaggedByBookingFlightId(flightId);
    }

    /** Returns all reviews for a given hotel, optionally sorted by helpful votes */
    public List<Review> getReviewsByHotel(Long hotelId, boolean sortByHelpful) {
        return sortByHelpful
            ? reviewRepository.findUnflaggedByBookingHotelIdSortedByHelpful(hotelId)
            : reviewRepository.findUnflaggedByBookingHotelId(hotelId);
    }

    /** Returns all reviews for a given hotel */
    public List<Review> getReviewsByHotel(Long hotelId) {
        return reviewRepository.findUnflaggedByBookingHotelId(hotelId);
    }

    /** Returns all reviews written by a user (by userId) */
    public List<Review> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    /** Returns all reviews written by a user (by username) */
    public List<Review> getReviewsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return reviewRepository.findByUserId(user.getId());
    }

    /** Returns the average star rating for a flight (null if no reviews) */
    public Double getAverageFlightRating(Long flightId) {
        return reviewRepository.averageRatingByFlightId(flightId);
    }

    /** Returns the average star rating for a hotel (null if no reviews) */
    public Double getAverageHotelRating(Long hotelId) {
        return reviewRepository.averageRatingByHotelId(hotelId);
    }

    /**
     * Updates an existing review.
     * Only the author can update their own review.
     */
    @Transactional
    public Review updateReview(String username, Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found: " + reviewId));

        if (!review.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only edit your own reviews");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setImageUrl(request.getImageUrl());
        return reviewRepository.save(review);
    }

    /** Deletes a review – only the author or an admin may delete */
    @Transactional
    public void deleteReview(String username, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found: " + reviewId));

        if (!review.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    /**
     * Marks a review as helpful (Feature 2).
     * Any authenticated user can vote; duplicate detection is left for a future
     * "review_helpful_votes" join-table when user-level deduplication is needed.
     *
     * @param reviewId the review to mark as helpful
     */
    @Transactional
    public Review markHelpful(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NoSuchElementException("Review not found: " + reviewId);
        }
        reviewRepository.incrementHelpfulVotes(reviewId);
        return reviewRepository.findById(reviewId).orElseThrow();
    }

    /**
     * Flags a review as inappropriate for moderation (Feature 2).
     *
     * @param reviewId the review to flag
     */
    @Transactional
    public Review flagReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NoSuchElementException("Review not found: " + reviewId);
        }
        reviewRepository.flagReview(reviewId);
        return reviewRepository.findById(reviewId).orElseThrow();
    }

    /** Returns all flagged reviews awaiting admin moderation */
    public List<Review> getFlaggedReviews() {
        return reviewRepository.findFlaggedReviews();
    }

    /** Unflag a review (admin action – restores it to public visibility) */
    @Transactional
    public Review unflagReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found: " + reviewId));
        review.setFlagged(false);
        return reviewRepository.save(review);
    }
}
