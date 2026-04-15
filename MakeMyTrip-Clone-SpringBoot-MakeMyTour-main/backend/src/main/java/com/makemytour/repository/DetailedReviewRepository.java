package com.makemytour.repository;

import com.makemytour.entity.DetailedReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for the multi-dimensional DetailedReview entity.
 * Feature 6 – Rate & Review System (RRS).
 */
@Repository
public interface DetailedReviewRepository extends JpaRepository<DetailedReview, Long> {

    /**
     * Returns all detailed reviews for a given flight (via Booking join).
     */
    @Query("SELECT dr FROM DetailedReview dr " +
           "JOIN dr.booking b WHERE b.flight.id = :flightId " +
           "ORDER BY dr.createdAt DESC")
    List<DetailedReview> findByFlightId(@Param("flightId") Long flightId);

    /**
     * Returns all detailed reviews for a given hotel (via Booking join).
     */
    @Query("SELECT dr FROM DetailedReview dr " +
           "JOIN dr.booking b WHERE b.hotel.id = :hotelId " +
           "ORDER BY dr.createdAt DESC")
    List<DetailedReview> findByHotelId(@Param("hotelId") Long hotelId);

    /** Returns all reviews written by a specific user */
    List<DetailedReview> findByUserId(Long userId);
}
