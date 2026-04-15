package com.makemytour.entity;

import com.makemytour.enums.TravelCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete TravelProduct representing a hotel.
 * Overrides calculatePrice() with weekend/holiday surge pricing.
 */
@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Hotel extends TravelProduct {

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address;

    /** Star rating of the hotel (1–5) */
    @Column(nullable = false)
    private int starRating;

    /** Total rooms available in the hotel */
    @Column(nullable = false)
    private int totalRooms;

    /** Grid dimensions for the room-selection component */
    @Column(nullable = false)
    private int roomRows;

    @Column(nullable = false)
    private int roomCols;

    /** Set of already-booked room IDs (e.g. "101", "203") */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_booked_rooms", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "room_id")
    private Set<String> bookedRooms = new HashSet<>();

    /** Comma-separated amenities (e.g. "WiFi,Pool,Gym") */
    private String amenities;

    @Builder
    public Hotel(Long id, String name, String description, Double basePrice,
                 TravelCategory category, String city, String address,
                 int starRating, int totalRooms, int roomRows, int roomCols,
                 String amenities) {
        super(id, name, description, basePrice, category, null);
        this.city = city;
        this.address = address;
        this.starRating = starRating;
        this.totalRooms = totalRooms;
        this.roomRows = roomRows;
        this.roomCols = roomCols;
        this.amenities = amenities;
    }

    @Override
    public String getProductType() {
        return "HOTEL";
    }

    /**
     * Applies 20% surge on weekends/holidays.
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
