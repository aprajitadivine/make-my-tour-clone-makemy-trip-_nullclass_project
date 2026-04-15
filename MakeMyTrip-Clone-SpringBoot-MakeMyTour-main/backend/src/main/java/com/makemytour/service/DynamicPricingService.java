package com.makemytour.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Feature 5 – Dynamic Pricing Engine
 *
 * Applies a 20% price surge when:
 *  1. The booking date falls on a weekend (Saturday or Sunday), OR
 *  2. The booking date falls on a major public holiday
 *
 * The holiday list is pre-seeded with common Indian public holidays.
 * In production this would be driven by a configurable database table.
 */
@Service
public class DynamicPricingService {

    /**
     * Surge multiplier applied on weekends and holidays (20% increase).
     */
    private static final double SURGE_MULTIPLIER = 1.20;

    /**
     * Hard-coded list of major Indian public holidays (month/day only –
     * year is ignored so the logic works for any year).
     */
    private static final Set<String> HOLIDAYS = Set.of(
        "01-26",   // Republic Day
        "08-15",   // Independence Day
        "10-02",   // Gandhi Jayanti
        "12-25",   // Christmas
        "11-01",   // Diwali (approximate – varies yearly)
        "03-29",   // Holi (approximate – varies yearly)
        "04-14",   // Dr. Ambedkar Jayanti
        "10-24",   // Dussehra (approximate)
        "01-01"    // New Year's Day
    );

    /**
     * Calculates the effective price for a given base price and booking date.
     *
     * @param basePrice   the base product price
     * @param bookingDate the date/time at which the customer is booking
     * @return surged price if weekend/holiday, otherwise the base price
     */
    public double calculatePrice(double basePrice, LocalDateTime bookingDate) {
        if (isSurgeDay(bookingDate.toLocalDate())) {
            return Math.round(basePrice * SURGE_MULTIPLIER * 100.0) / 100.0;
        }
        return basePrice;
    }

    /**
     * Returns true if the given date is a weekend or a recognised holiday.
     *
     * @param date the booking date
     * @return true if surge pricing should apply
     */
    public boolean isSurgeDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        boolean isHoliday = HOLIDAYS.contains(
            String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth())
        );
        return isWeekend || isHoliday;
    }

    /**
     * Returns the surge multiplier (e.g. 1.20 for a 20% surge).
     * Used by the frontend to display the "surge active" badge.
     *
     * @param bookingDate the booking date
     * @return the effective multiplier (1.0 = no surge, 1.20 = 20% surge)
     */
    public double getSurgeMultiplier(LocalDateTime bookingDate) {
        return isSurgeDay(bookingDate.toLocalDate()) ? SURGE_MULTIPLIER : 1.0;
    }
}
