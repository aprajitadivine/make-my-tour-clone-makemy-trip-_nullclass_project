package com.makemytour.event;

import com.makemytour.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent published when a booking is confirmed (TICKET_CONFIRMED).
 * The NotificationService listens for this event and triggers the fail-safe
 * delivery pipeline: Push (FCM) → SMS (Twilio) → Email (Nodemailer equivalent).
 */
@Getter
public class TicketConfirmedEvent extends ApplicationEvent {

    private final Booking booking;
    private final String recipientEmail;
    private final String recipientPhone;

    public TicketConfirmedEvent(Object source, Booking booking,
                                String recipientEmail, String recipientPhone) {
        super(source);
        this.booking = booking;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
    }
}
