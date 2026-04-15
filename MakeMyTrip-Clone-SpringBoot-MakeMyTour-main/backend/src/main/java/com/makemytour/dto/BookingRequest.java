package com.makemytour.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** Request body for POST /api/bookings */
@Data
public class BookingRequest {

    /** ID of the flight to book (null for hotel bookings) */
    private Long flightId;

    /** ID of the hotel to book (null for flight bookings) */
    private Long hotelId;

    @NotNull(message = "Travel/check-in date is required")
    private LocalDateTime travelDate;

    /** Check-out date for hotel bookings */
    private LocalDateTime returnDate;

    /**
     * Seat or room ID selected from the React SeatMap grid.
     * Example: "3A" for a flight seat, "101" for a hotel room.
     */
    private String seatId;

    /**
     * Travel category preference (optional – used to seed AI recommendations).
     * When omitted the category is inferred from the flight/hotel entity.
     */
    private String preferredCategory;
}
