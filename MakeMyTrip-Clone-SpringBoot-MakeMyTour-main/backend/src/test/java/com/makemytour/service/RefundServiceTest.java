package com.makemytour.service;

import com.makemytour.entity.Booking;
import com.makemytour.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Feature 1 – Refund Logic.
 * No Spring context needed – pure unit test.
 */
class RefundServiceTest {

    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundService();
    }

    @Test
    void shouldReturnFullRefundWhenCancelledMoreThan24HoursBeforeDeparture() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .totalPrice(5000.0)
                .travelDate(LocalDateTime.now().plusHours(48))
                .build();

        double refund = refundService.calculateRefund(booking);

        assertThat(refund).isEqualTo(5000.0);
    }

    @Test
    void shouldReturnHalfRefundWhenCancelledLessThan24HoursBeforeDeparture() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .totalPrice(5000.0)
                .travelDate(LocalDateTime.now().plusHours(10)) // < 24h
                .build();

        double refund = refundService.calculateRefund(booking);

        assertThat(refund).isEqualTo(2500.0);
    }

    @Test
    void shouldThrowExceptionForNonConfirmedBooking() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CANCELLED)
                .totalPrice(5000.0)
                .travelDate(LocalDateTime.now().plusDays(3))
                .build();

        assertThatThrownBy(() -> refundService.calculateRefund(booking))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void shouldReturnTrueForCancellableBooking() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .travelDate(LocalDateTime.now().plusDays(2))
                .build();

        assertThat(refundService.isCancellable(booking)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDepartureHasPassed() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .travelDate(LocalDateTime.now().minusHours(1))
                .build();

        assertThat(refundService.isCancellable(booking)).isFalse();
    }
}
