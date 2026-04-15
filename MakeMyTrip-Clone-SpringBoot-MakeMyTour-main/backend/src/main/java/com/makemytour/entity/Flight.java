package com.makemytour.entity;

import com.makemytour.enums.FlightStatus;
import com.makemytour.enums.TravelCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete TravelProduct representing a scheduled flight.
 * Overrides calculatePrice() to apply a 20% weekend/holiday surge.
 */
@Entity
@Table(name = "flights")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Flight extends TravelProduct {

    @Column(nullable = false, unique = true)
    private String flightNumber;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    /** UTC departure time used for refund calculations */
    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    /** Live status toggled by FlightStatusService */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus status = FlightStatus.ON_TIME;

    /**
     * Delay in minutes (Feature 3).
     * Populated by FlightStatusService when status = DELAYED.
     * 0 when on time.
     */
    @Column(nullable = false)
    private int delayMinutes = 0;

    /**
     * Human-readable reason for the delay (Feature 3).
     * Examples: "Air traffic congestion", "Technical check", "Weather conditions".
     * null when on time.
     */
    private String delayReason;

    /** Total seat rows (e.g. 30 for an A320) */
    @Column(nullable = false)
    private int seatRows;

    /** Total seat columns (e.g. 6 for an A320: A-F) */
    @Column(nullable = false)
    private int seatCols;

    /** Set of already-booked seat IDs (e.g. "3A", "12C") */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "flight_booked_seats", joinColumns = @JoinColumn(name = "flight_id"))
    @Column(name = "seat_id")
    private Set<String> bookedSeats = new HashSet<>();

    /**
     * Set of premium seat IDs (Feature 4 – premium seat upselling).
     * These seats are highlighted on the SeatMap and shown with a surcharge.
     * Examples: first-row seats, exit-row seats, aisle seats near front.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "flight_premium_seats", joinColumns = @JoinColumn(name = "flight_id"))
    @Column(name = "seat_id")
    private Set<String> premiumSeats = new HashSet<>();

    @Builder
    public Flight(Long id, String name, String description, Double basePrice,
                  TravelCategory category, String flightNumber, String origin,
                  String destination, LocalDateTime departureTime,
                  LocalDateTime arrivalTime, int seatRows, int seatCols) {
        super(id, name, description, basePrice, category, null);
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.seatRows = seatRows;
        this.seatCols = seatCols;
        this.status = FlightStatus.ON_TIME;
    }

    @Override
    public String getProductType() {
        return "FLIGHT";
    }

    /**
     * Applies a 20% price surge if the booking date falls on a weekend or holiday.
     * Core logic is duplicated here for standalone use; the DynamicPricingService
     * provides the authoritative calculation when the service layer is available.
     */
    @Override
    public Double calculatePrice(LocalDateTime bookingDate) {
        DayOfWeek day = bookingDate.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return getBasePrice() * 1.20;
        }
        return getBasePrice();
    }
}
