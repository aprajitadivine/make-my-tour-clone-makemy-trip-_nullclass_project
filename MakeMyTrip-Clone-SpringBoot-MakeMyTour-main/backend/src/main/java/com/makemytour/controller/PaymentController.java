package com.makemytour.controller;

import com.makemytour.dto.BookingRequest;
import com.makemytour.dto.PaymentOrderRequest;
import com.makemytour.dto.PaymentVerifyRequest;
import com.makemytour.entity.Booking;
import com.makemytour.service.BookingService;
import com.makemytour.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST API for Razorpay payment integration.
 *
 * Flow:
 *  1. POST /api/payments/create-order  – backend creates a Razorpay order, returns order_id + key_id
 *  2. Frontend opens the Razorpay checkout widget using the order_id
 *  3. POST /api/payments/verify        – backend verifies signature, then creates the booking
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    /**
     * POST /api/payments/create-order
     *
     * Creates a Razorpay order for the given amount (in INR rupees).
     * Returns the order ID and publishable key so the frontend can open the checkout widget.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PaymentOrderRequest request) {
        try {
            Order order = paymentService.createOrder(request.getAmount(), request.getReceipt());
            return ResponseEntity.ok(Map.of(
                "orderId",   order.get("id"),
                "amount",    order.get("amount"),   // in paise
                "currency",  order.get("currency"),
                "keyId",     paymentService.getKeyId(),
                "testMode",  paymentService.isTestMode()
            ));
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Payment gateway error. Please try again."));
        }
    }

    /**
     * POST /api/payments/verify
     *
     * 1. Verifies the HMAC-SHA256 signature from Razorpay.
     * 2. On success, creates and persists the booking.
     * 3. Returns the created Booking object.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PaymentVerifyRequest request) {

        // Step 1: Verify the payment signature
        boolean valid = paymentService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Payment verification failed. Invalid signature."));
        }

        // Step 2: Build BookingRequest from the payment verify payload
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setFlightId(request.getFlightId());
        bookingRequest.setHotelId(request.getHotelId());
        bookingRequest.setSeatId(request.getSeatId());

        // Parse ISO-8601 / datetime-local strings → LocalDateTime
        try {
            if (request.getTravelDate() != null && !request.getTravelDate().isBlank()) {
                bookingRequest.setTravelDate(parseDateTime(request.getTravelDate()));
            }
            if (request.getReturnDate() != null && !request.getReturnDate().isBlank()) {
                bookingRequest.setReturnDate(parseDateTime(request.getReturnDate()));
            }
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid date format. Expected YYYY-MM-DDTHH:mm or YYYY-MM-DDTHH:mm:ss"));
        }

        // Step 3: Create the booking
        try {
            Booking booking = bookingService.createBooking(userDetails.getUsername(), bookingRequest);
            log.info("Booking {} created after successful Razorpay payment {} for user {}",
                    booking.getId(), request.getRazorpayPaymentId(), userDetails.getUsername());
            return ResponseEntity.ok(booking);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Parses a datetime-local string ("YYYY-MM-DDTHH:mm" or "YYYY-MM-DDTHH:mm:ss") */
    private LocalDateTime parseDateTime(String dt) {
        // datetime-local gives 16-char "YYYY-MM-DDTHH:mm"; LocalDateTime needs seconds
        if (dt.length() == 16) {
            dt = dt + ":00";
        }
        return LocalDateTime.parse(dt);
    }
}
