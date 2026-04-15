package com.makemytour.dto;

import lombok.Data;

/**
 * Request body sent by the frontend after the Razorpay payment modal closes successfully.
 * Contains both the Razorpay payment identifiers (for signature verification) and the
 * original booking details needed to persist the booking.
 */
@Data
public class PaymentVerifyRequest {

    // ── Razorpay payment identifiers ─────────────────────────────
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;

    // ── Booking details (mirrors BookingRequest) ─────────────────
    private Long flightId;
    private Long hotelId;
    private String travelDate;
    private String returnDate;
    private String seatId;
}
