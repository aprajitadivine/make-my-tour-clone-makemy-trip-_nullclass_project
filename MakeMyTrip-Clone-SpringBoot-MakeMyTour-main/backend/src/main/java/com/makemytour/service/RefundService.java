package com.makemytour.service;

import com.makemytour.entity.Booking;
import com.makemytour.enums.BookingStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Feature 1 – Refund Logic
 *
 * Business rule:
 *  - If cancellation occurs LESS THAN 24 hours before departure → 50% refund
 *  - If cancellation occurs 24 hours or MORE before departure  → 100% refund
 *
 * This service is intentionally stateless so it can be unit-tested without a
 * Spring context (SOLID: Single Responsibility Principle).
 */
@Service
public class RefundService {

    /**
     * Calculates the refund amount for a cancellation made at the current time.
     *
     * @param booking  the booking being cancelled
     * @return the refund amount in the same currency as totalPrice
     * @throws IllegalArgumentException if the booking is not in CONFIRMED state
     */
    public double calculateRefund(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                "Refund can only be calculated for CONFIRMED bookings. " +
                "Current status: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = booking.getTravelDate();

        // Number of hours remaining until departure
        long hoursUntilDeparture = Duration.between(now, departure).toHours();

        if (hoursUntilDeparture < 24) {
            // Late cancellation – only 50% is refunded
            return booking.getTotalPrice() * 0.50;
        } else {
            // Full refund for cancellations made well in advance
            return booking.getTotalPrice();
        }
    }

    /**
     * Determines whether a booking is eligible for cancellation.
     * Cancellation is blocked after the departure/check-in time has passed.
     *
     * @param booking the booking to check
     * @return true if the booking can still be cancelled
     */
    public boolean isCancellable(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
               && LocalDateTime.now().isBefore(booking.getTravelDate());
    }
}
