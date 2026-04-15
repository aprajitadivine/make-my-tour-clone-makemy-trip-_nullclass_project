package com.makemytour.repository;

import com.makemytour.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookingFlightId(Long flightId);

    List<Review> findByBookingHotelId(Long hotelId);

    List<Review> findByUserId(Long userId);

    /** Reviews for a flight sorted by most helpful first (Feature 2) */
    List<Review> findByBookingFlightIdOrderByHelpfulVotesDesc(Long flightId);

    /** Reviews for a hotel sorted by most helpful first (Feature 2) */
    List<Review> findByBookingHotelIdOrderByHelpfulVotesDesc(Long hotelId);

    /** Unflagged reviews for a flight (public feed) */
    @Query("SELECT r FROM Review r WHERE r.booking.flight.id = :flightId AND r.flagged = false")
    List<Review> findUnflaggedByBookingFlightId(Long flightId);

    /** Unflagged reviews for a flight sorted by helpful votes (public feed) */
    @Query("SELECT r FROM Review r WHERE r.booking.flight.id = :flightId AND r.flagged = false ORDER BY r.helpfulVotes DESC")
    List<Review> findUnflaggedByBookingFlightIdSortedByHelpful(Long flightId);

    /** Unflagged reviews for a hotel (public feed) */
    @Query("SELECT r FROM Review r WHERE r.booking.hotel.id = :hotelId AND r.flagged = false")
    List<Review> findUnflaggedByBookingHotelId(Long hotelId);

    /** Unflagged reviews for a hotel sorted by helpful votes (public feed) */
    @Query("SELECT r FROM Review r WHERE r.booking.hotel.id = :hotelId AND r.flagged = false ORDER BY r.helpfulVotes DESC")
    List<Review> findUnflaggedByBookingHotelIdSortedByHelpful(Long hotelId);

    /** All flagged reviews for admin moderation */
    @Query("SELECT r FROM Review r WHERE r.flagged = true")
    List<Review> findFlaggedReviews();

    /** Average star rating for a flight */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.booking.flight.id = :flightId")
    Double averageRatingByFlightId(Long flightId);

    /** Average star rating for a hotel */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.booking.hotel.id = :hotelId")
    Double averageRatingByHotelId(Long hotelId);

    /** Increment helpful vote count (Feature 2) */
    @Modifying
    @Query("UPDATE Review r SET r.helpfulVotes = r.helpfulVotes + 1 WHERE r.id = :reviewId")
    void incrementHelpfulVotes(Long reviewId);

    /** Mark a review as flagged (Feature 2) */
    @Modifying
    @Query("UPDATE Review r SET r.flagged = true WHERE r.id = :reviewId")
    void flagReview(Long reviewId);
}
