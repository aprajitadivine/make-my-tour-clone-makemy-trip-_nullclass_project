package com.makemytour.entity;

import com.makemytour.enums.TravelCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Abstract base class for all travel products (Flights, Hotels).
 * Demonstrates the SOLID Open/Closed Principle and OOP Inheritance.
 *
 * Concrete subclasses inherit common fields and must implement
 * getProductType() and calculatePrice() to demonstrate Polymorphism.
 */
@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class TravelProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name of the product */
    @Column(nullable = false)
    private String name;

    /** Detailed description shown on the listing page */
    @Column(length = 1000)
    private String description;

    /** Base price before dynamic pricing is applied */
    @Column(nullable = false)
    private Double basePrice;

    /** Category drives the AI Recommendation engine */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelCategory category;

    /** Timestamp when this product record was created */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // -------------------------------------------------------
    // Polymorphic contract – subclasses must implement these
    // -------------------------------------------------------

    /** Returns a human-readable product type label (e.g. "FLIGHT", "HOTEL") */
    public abstract String getProductType();

    /**
     * Calculates the effective price for a given booking date.
     * Subclasses delegate to DynamicPricingService for surge logic.
     *
     * @param bookingDate the date/time at which the booking is made
     * @return the final price after any surge adjustments
     */
    public abstract Double calculatePrice(LocalDateTime bookingDate);
}
