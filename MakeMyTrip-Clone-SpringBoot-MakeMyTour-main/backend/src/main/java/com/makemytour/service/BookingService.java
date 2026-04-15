package com.makemytour.service;

import com.makemytour.dto.BookingRequest;
import com.makemytour.entity.*;
import com.makemytour.enums.BookingStatus;
import com.makemytour.event.TicketConfirmedEvent;
import com.makemytour.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Core booking workflow:
 *  - Create bookings for flights or hotels
 *  - Apply dynamic pricing at booking time
 *  - Reserve seat/room in the grid (Feature 4 – Seat/Room Grid)
 *  - Cancel bookings and trigger refund calculations (Feature 1)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final DynamicPricingService pricingService;
    private final RefundService refundService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new booking.
     *
     * Steps:
     *  1. Validate exactly one of flightId / hotelId is provided.
     *  2. Verify the selected seat/room is not already booked.
     *  3. Calculate the final price using DynamicPricingService.
     *  4. Mark the seat/room as booked in the flight/hotel entity.
     *  5. Persist the Booking record.
     *
     * @param username current authenticated user
     * @param request  booking data from the frontend
     * @return the persisted Booking
     */
    @Transactional
    public Booking createBooking(String username, BookingRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        if (request.getFlightId() == null && request.getHotelId() == null) {
            throw new IllegalArgumentException("Either flightId or hotelId must be provided");
        }
        if (request.getFlightId() != null && request.getHotelId() != null) {
            throw new IllegalArgumentException("Cannot book a flight and hotel in the same request");
        }

        LocalDateTime bookingDate = LocalDateTime.now();
        Booking.BookingBuilder builder = Booking.builder()
                .user(user)
                .bookingDate(bookingDate)
                .travelDate(request.getTravelDate())
                .returnDate(request.getReturnDate())
                .seatId(request.getSeatId())
                .status(BookingStatus.CONFIRMED);

        if (request.getFlightId() != null) {
            builder = buildFlightBooking(builder, request, bookingDate);
        } else {
            builder = buildHotelBooking(builder, request, bookingDate);
        }

        Booking booking = bookingRepository.save(builder.build());
        log.info("Booking {} created for user {} (seat/room: {})",
                booking.getId(), username, booking.getSeatId());

        // Publish TICKET_CONFIRMED event – triggers the async notification pipeline
        eventPublisher.publishEvent(new TicketConfirmedEvent(
                this, booking, user.getEmail(), user.getPhoneNumber()));

        return booking;
    }

    private Booking.BookingBuilder buildFlightBooking(Booking.BookingBuilder builder,
                                                      BookingRequest request,
                                                      LocalDateTime bookingDate) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Flight not found: " + request.getFlightId()));

        // Validate seat availability
        if (request.getSeatId() != null && flight.getBookedSeats().contains(request.getSeatId())) {
            throw new IllegalStateException("Seat " + request.getSeatId() + " is already booked");
        }

        // Apply dynamic pricing
        double finalPrice = pricingService.calculatePrice(flight.getBasePrice(), bookingDate);

        // Reserve the seat
        if (request.getSeatId() != null) {
            flight.getBookedSeats().add(request.getSeatId());
            flightRepository.save(flight);
        }

        return builder
                .flight(flight)
                .totalPrice(finalPrice)
                .category(flight.getCategory());
    }

    private Booking.BookingBuilder buildHotelBooking(Booking.BookingBuilder builder,
                                                     BookingRequest request,
                                                     LocalDateTime bookingDate) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Hotel not found: " + request.getHotelId()));

        // Validate room availability
        if (request.getSeatId() != null && hotel.getBookedRooms().contains(request.getSeatId())) {
            throw new IllegalStateException("Room " + request.getSeatId() + " is already booked");
        }

        // Apply dynamic pricing
        double finalPrice = pricingService.calculatePrice(hotel.getBasePrice(), bookingDate);

        // Reserve the room
        if (request.getSeatId() != null) {
            hotel.getBookedRooms().add(request.getSeatId());
            hotelRepository.save(hotel);
        }

        return builder
                .hotel(hotel)
                .totalPrice(finalPrice)
                .category(hotel.getCategory());
    }

    /**
     * Cancels a booking and calculates the applicable refund.
     *
     * @param username           current authenticated user (must own the booking)
     * @param bookingId          the booking to cancel
     * @param cancellationReason optional free-text / dropdown reason
     * @return the cancelled Booking (contains refundStatus, travelDate, totalPrice, etc.)
     */
    @Transactional
    public Booking cancelBooking(String username, Long bookingId, String cancellationReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new SecurityException("You can only cancel your own bookings");
        }

        if (!refundService.isCancellable(booking)) {
            throw new IllegalStateException(
                "Booking cannot be cancelled. Status: " + booking.getStatus() +
                ". Departure may have already passed.");
        }

        double refundAmount = refundService.calculateRefund(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(cancellationReason);
        booking.setRefundStatus("PENDING_REFUND");
        // Store the computed refund so the controller can read it without re-fetching
        booking.setTotalPrice(refundAmount);

        // Release the seat/room so it can be booked by others
        if (booking.getSeatId() != null) {
            if (booking.getFlight() != null) {
                booking.getFlight().getBookedSeats().remove(booking.getSeatId());
                flightRepository.save(booking.getFlight());
            } else if (booking.getHotel() != null) {
                booking.getHotel().getBookedRooms().remove(booking.getSeatId());
                hotelRepository.save(booking.getHotel());
            }
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} cancelled (reason: {}). Refund: ₹{}", bookingId, cancellationReason, refundAmount);
        return saved;
    }

    /** Returns all bookings for the authenticated user */
    public List<Booking> getMyBookings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return bookingRepository.findByUserId(user.getId());
    }

    /** Returns a specific booking (ownership check included) */
    public Booking getBookingById(String username, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new SecurityException("Access denied to booking " + bookingId);
        }

        return booking;
    }
}
