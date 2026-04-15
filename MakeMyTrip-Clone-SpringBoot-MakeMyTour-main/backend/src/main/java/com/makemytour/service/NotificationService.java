package com.makemytour.service;

import com.makemytour.entity.Booking;
import com.makemytour.event.TicketConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Advanced Notification System (ANS) – Feature 1.
 *
 * Implements a fail-safe delivery pipeline:
 *   Push (FCM) → SMS (Twilio) → Email (Nodemailer/Spring Mail)
 *
 * Each channel is attempted in order; if a channel fails, the next one is tried.
 * The pipeline runs asynchronously on the "notificationExecutor" thread pool so
 * that booking confirmation HTTP responses are never blocked.
 *
 * NOTE: Channel implementations are intentionally stubbed with log statements.
 *       In production, replace each stub with the real SDK call (FCM Admin SDK,
 *       Twilio REST client, Spring JavaMailSender) and inject credentials from
 *       environment variables / secrets manager – never from source control.
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * Listens for TICKET_CONFIRMED events and triggers the fail-safe pipeline.
     * The @Async annotation ensures this runs in a separate thread (notificationExecutor).
     */
    @Async("notificationExecutor")
    @EventListener
    public void handleTicketConfirmed(TicketConfirmedEvent event) {
        Booking booking = event.getBooking();
        log.info("[ANS] TICKET_CONFIRMED event received for booking id={}", booking.getId());

        if (sendPushNotification(booking)) {
            log.info("[ANS] Push (FCM) delivered for booking id={}", booking.getId());
            return;
        }
        log.warn("[ANS] Push (FCM) failed for booking id={}; falling back to SMS", booking.getId());

        if (sendSmsNotification(event.getRecipientPhone(), booking)) {
            log.info("[ANS] SMS (Twilio) delivered for booking id={}", booking.getId());
            return;
        }
        log.warn("[ANS] SMS (Twilio) failed for booking id={}; falling back to Email", booking.getId());

        if (sendEmailNotification(event.getRecipientEmail(), booking)) {
            log.info("[ANS] Email delivered for booking id={}", booking.getId());
        } else {
            log.error("[ANS] All notification channels failed for booking id={}", booking.getId());
        }
    }

    /**
     * Push notification via Firebase Cloud Messaging (FCM).
     * Stub: replace with FCM Admin SDK call in production.
     *
     * @return true if delivery succeeded, false if it should fall back
     */
    private boolean sendPushNotification(Booking booking) {
        try {
            log.debug("[ANS][FCM] Sending push notification for booking id={}", booking.getId());
            // Production: FirebaseMessaging.getInstance().send(message)
            // Simulate success – swap to false to test fallback chain in dev
            return true;
        } catch (Exception ex) {
            log.error("[ANS][FCM] Exception: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * SMS notification via Twilio.
     * Stub: replace with Twilio REST client call in production.
     *
     * @return true if delivery succeeded, false if it should fall back
     */
    private boolean sendSmsNotification(String phone, Booking booking) {
        try {
            if (phone == null || phone.isBlank()) {
                log.warn("[ANS][SMS] No phone number for booking id={}", booking.getId());
                return false;
            }
            log.debug("[ANS][Twilio] Sending SMS to {} for booking id={}", phone, booking.getId());
            // Production: Message.creator(new PhoneNumber(phone), ...).create()
            return true;
        } catch (Exception ex) {
            log.error("[ANS][SMS] Exception: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Email notification via Spring JavaMailSender (Nodemailer equivalent in Java).
     * Stub: replace with JavaMailSender.send(MimeMessage) in production.
     *
     * @return true if delivery succeeded
     */
    private boolean sendEmailNotification(String email, Booking booking) {
        try {
            if (email == null || email.isBlank()) {
                log.warn("[ANS][Email] No email address for booking id={}", booking.getId());
                return false;
            }
            log.debug("[ANS][Email] Sending confirmation email to {} for booking id={}", email, booking.getId());
            // Production: javaMailSender.send(mimeMessageHelper.getMimeMessage())
            return true;
        } catch (Exception ex) {
            log.error("[ANS][Email] Exception: {}", ex.getMessage());
            return false;
        }
    }
}
