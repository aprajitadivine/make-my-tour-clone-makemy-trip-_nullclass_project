package com.makemytour.dto;

import lombok.Data;

/**
 * Request body for creating a Razorpay order.
 * The amount is the total price in INR (will be converted to paise for Razorpay).
 */
@Data
public class PaymentOrderRequest {

    /** Total amount in INR rupees (e.g. 4999.0) */
    private double amount;

    /** Optional receipt / reference (e.g. "flight-42") */
    private String receipt;
}
