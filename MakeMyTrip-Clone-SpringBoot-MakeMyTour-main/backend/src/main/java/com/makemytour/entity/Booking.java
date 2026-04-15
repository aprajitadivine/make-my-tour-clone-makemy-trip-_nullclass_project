package com.makemytour.entity;

import com.makemytour.enums.BookingStatus;
import com.makemytour.enums.TravelCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a user's booking for either a Flight or a Hotel.
 * Uses nullable FKs to both Flight and Hotel; exactly one must be non-null.
 *
 * The seatId / roomId field stores the selection from the React SeatMap grid.
 */
@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Populated for flight bookings (null for hotel bookings) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    /** Populated for hotel bookings (null for flight bookings) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    /** Date/time the booking was created */
    @Column(nullable = false)
    private LocalDateTime bookingDate;

    /** Departure/check-in date – used for refund calculations */
    @Column(nullable = false)
    private LocalDateTime travelDate;

    /** Check-out date (hotels only) */
    private LocalDateTime returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    /**
     * Seat ID (flight) or room ID (hotel) selected via the SeatMap component.
     * Example: "3A" for a flight seat, "101" for a hotel room.
     */
    private String seatId;

    /** Final price charged – already includes dynamic pricing surge */
    @Column(nullable = false)
    private Double totalPrice;

    /**
     * Mirrors the category of the booked product.
     * Stored here so the AI Recommendation engine can query it directly
     * without joining back to Flight/Hotel.
     */
    @Enumerated(EnumType.STRING)
    private TravelCategory category;

    /**
     * Optional reason the user chose when cancelling (e.g. "Change of plans").
     * Persisted so the operations team can analyse cancellation trends.
     */
    private String cancellationReason;

    /**
     * Tracks refund lifecycle: null = no refund initiated,
     * PENDING_REFUND = cancellation accepted but refund not yet processed,
     * REFUNDED = refund successfully issued.
     */
    private String refundStatus;

    @PrePersist
    protected void onCreate() {
        if (bookingDate == null) {
            bookingDate = LocalDateTime.now();
        }
    }
}
