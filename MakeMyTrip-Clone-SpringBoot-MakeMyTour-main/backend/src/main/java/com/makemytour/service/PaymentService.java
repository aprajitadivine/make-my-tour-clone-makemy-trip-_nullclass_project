package com.makemytour.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Wraps Razorpay API calls:
 *  - Create an order (amount in paise)
 *  - Verify the HMAC-SHA256 payment signature returned by the checkout widget
 *
 * When RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET environment variables are not set
 * the service operates in <em>demo/test mode</em>: orders are mocked locally and
 * signature verification always succeeds.  This lets the full booking flow work
 * in development and demo environments without real Razorpay credentials.
 */
@Service
@Slf4j
public class PaymentService {

    private static final String PLACEHOLDER_KEY_ID = "rzp_test_placeholder";
    private static final String DEMO_ORDER_PREFIX   = "order_demo_";

    private final RazorpayClient razorpayClient;
    private final String keyId;
    private final String keySecret;
    /** True when no real Razorpay credentials are configured. */
    private final boolean testMode;

    public PaymentService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) throws RazorpayException {
        this.keyId    = keyId;
        this.keySecret = keySecret;

        boolean credentialsMissing = keyId == null || keyId.isBlank()
                || PLACEHOLDER_KEY_ID.equals(keyId)
                || keySecret == null || keySecret.isBlank()
                || "placeholder_secret".equals(keySecret);

        if (credentialsMissing) {
            this.testMode = true;
            this.razorpayClient = null;
            log.warn("Razorpay credentials are not configured – running in DEMO/TEST mode. " +
                     "Real payments are disabled. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET " +
                     "environment variables to enable live payments.");
        } else {
            this.testMode = false;
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
            log.info("Razorpay payment gateway initialised (key: {}...)", keyId.substring(0, Math.min(8, keyId.length())));
        }
    }

    /**
     * Creates a Razorpay order.
     * In demo/test mode a mock order is returned without calling the Razorpay API.
     *
     * @param amountInRupees total price in INR (e.g. 4999.0)
     * @param receipt        optional receipt tag shown in the Razorpay dashboard
     * @return the created Order object
     */
    public Order createOrder(double amountInRupees, String receipt) throws RazorpayException {
        // Use BigDecimal to avoid floating-point rounding errors.
        long amountInPaise = BigDecimal.valueOf(amountInRupees)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        if (testMode) {
            JSONObject mockOrder = new JSONObject();
            mockOrder.put("id", DEMO_ORDER_PREFIX + System.currentTimeMillis());
            mockOrder.put("amount", amountInPaise);
            mockOrder.put("currency", "INR");
            mockOrder.put("receipt", receipt != null ? receipt : "order_rcpt");
            log.info("Demo mode – returning mock order for ₹{}", amountInRupees);
            return new Order(mockOrder);
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt != null ? receipt : "order_rcpt");
        orderRequest.put("payment_capture", 1);

        Order order = razorpayClient.orders.create(orderRequest);
        log.info("Razorpay order created: {} for ₹{}", order.get("id"), amountInRupees);
        return order;
    }

    /**
     * Verifies the HMAC-SHA256 signature Razorpay sends after a successful payment.
     * The expected signature = HMAC-SHA256(orderId + "|" + paymentId, keySecret).
     * In demo/test mode the verification always succeeds.
     *
     * @return true if the signature is valid
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (testMode) {
            log.info("Demo mode – skipping Razorpay signature verification for order {}", orderId);
            return true;
        }
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            Utils.verifyPaymentSignature(attributes, keySecret);
            return true;
        } catch (RazorpayException e) {
            log.warn("Razorpay signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** Returns the Razorpay key id (safe to expose to the browser) */
    public String getKeyId() {
        return keyId;
    }

    /** Returns true when the service is running in demo/test mode (no real Razorpay credentials). */
    public boolean isTestMode() {
        return testMode;
    }
}
