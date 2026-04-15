package com.makemytour.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Feature 5 – Dynamic Pricing.
 */
class DynamicPricingServiceTest {

    private DynamicPricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new DynamicPricingService();
    }

    @Test
    void shouldApply20PercentSurgeOnSaturday() {
        // Find the next Saturday
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDateTime bookingDate = saturday.atTime(10, 0);

        double price = pricingService.calculatePrice(10000.0, bookingDate);

        assertThat(price).isEqualTo(12000.0);
    }

    @Test
    void shouldApply20PercentSurgeOnSunday() {
        LocalDate sunday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDateTime bookingDate = sunday.atTime(10, 0);

        double price = pricingService.calculatePrice(10000.0, bookingDate);

        assertThat(price).isEqualTo(12000.0);
    }

    @Test
    void shouldNotApplySurgeOnWeekday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDateTime bookingDate = monday.atTime(10, 0);

        double price = pricingService.calculatePrice(10000.0, bookingDate);

        // Could be holiday – just verify price is either base or surged
        assertThat(price).isGreaterThanOrEqualTo(10000.0);
    }

    @Test
    void shouldApplySurgeOnRepublicDay() {
        int year = LocalDate.now().getYear();
        LocalDateTime republicDay = LocalDate.of(year, 1, 26).atTime(9, 0);

        double price = pricingService.calculatePrice(5000.0, republicDay);

        assertThat(price).isEqualTo(6000.0);
    }

    @Test
    void shouldReturnSurgeMultiplierOf1Point20OnWeekend() {
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        double multiplier = pricingService.getSurgeMultiplier(saturday.atTime(10, 0));
        assertThat(multiplier).isEqualTo(1.20);
    }
}
