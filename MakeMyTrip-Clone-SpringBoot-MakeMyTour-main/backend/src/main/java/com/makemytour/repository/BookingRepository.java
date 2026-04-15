package com.makemytour.repository;

import com.makemytour.entity.Booking;
import com.makemytour.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    /** Fetches the most recent booking for a user – used by the AI Recommendation engine */
    Optional<Booking> findTopByUserIdOrderByBookingDateDesc(Long userId);

    List<Booking> findByFlightId(Long flightId);

    List<Booking> findByHotelId(Long hotelId);
}
