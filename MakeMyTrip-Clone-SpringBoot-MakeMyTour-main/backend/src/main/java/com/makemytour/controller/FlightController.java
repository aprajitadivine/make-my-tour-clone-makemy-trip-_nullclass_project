package com.makemytour.controller;

import com.makemytour.entity.Flight;
import com.makemytour.enums.FlightStatus;
import com.makemytour.service.DynamicPricingService;
import com.makemytour.service.FlightService;
import com.makemytour.service.FlightStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for flight search, status updates, and dynamic pricing.
 */
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final FlightStatusService flightStatusService;
    private final DynamicPricingService pricingService;

    /** GET /api/flights – Lists all available flights */
    @GetMapping
    public ResponseEntity<List<Flight>> getAllFlights() {
        return ResponseEntity.ok(flightService.getAllFlights());
    }

    /** GET /api/flights/{id} – Returns a single flight */
    @GetMapping("/{id}")
    public ResponseEntity<Flight> getFlightById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    /**
     * GET /api/flights/search – Searches flights by route and date.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Flight>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return ResponseEntity.ok(flightService.searchFlights(origin, destination, date));
    }

    /**
     * GET /api/flights/{id}/status – Returns the live status of a flight (Feature 3).
     * Response includes delay minutes, delay reason, and estimated arrival time.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getFlightStatus(@PathVariable Long id) {
        Flight flight = flightService.getFlightById(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("flightId", id);
        response.put("flightNumber", flight.getFlightNumber());
        response.put("status", flight.getStatus().name());
        response.put("delayMinutes", flight.getDelayMinutes());
        response.put("delayReason", flight.getDelayReason());

        // Estimated arrival = original arrival + delay
        LocalDateTime estimatedArrival = flight.getArrivalTime()
            .plusMinutes(flight.getDelayMinutes());
        response.put("scheduledArrival", flight.getArrivalTime().toString());
        response.put("estimatedArrival", estimatedArrival.toString());

        if (flight.getStatus() == FlightStatus.DELAYED && flight.getDelayMinutes() > 0) {
            response.put("statusMessage",
                "Delayed by " + flight.getDelayMinutes() + " min – " + flight.getDelayReason());
        } else if (flight.getStatus() == FlightStatus.CANCELLED) {
            response.put("statusMessage", "Flight cancelled");
        } else {
            response.put("statusMessage", "On time");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/flights/{id}/status/refresh – Randomly refreshes the flight status (Feature 3).
     * Simulates a real-time status board update with delay info.
     */
    @PutMapping("/{id}/status/refresh")
    public ResponseEntity<Flight> refreshFlightStatus(@PathVariable Long id) {
        return ResponseEntity.ok(flightStatusService.updateFlightStatus(id));
    }

    /**
     * GET /api/flights/{id}/pricing – Returns the dynamic price for a booking date (Feature 5).
     */
    @GetMapping("/{id}/pricing")
    public ResponseEntity<Map<String, Object>> getDynamicPrice(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDate) {

        Flight flight = flightService.getFlightById(id);
        double dynamicPrice = pricingService.calculatePrice(flight.getBasePrice(), bookingDate);
        double surgeMultiplier = pricingService.getSurgeMultiplier(bookingDate);
        boolean isSurge = surgeMultiplier > 1.0;

        return ResponseEntity.ok(Map.of(
            "flightId", id,
            "basePrice", flight.getBasePrice(),
            "dynamicPrice", dynamicPrice,
            "surgeMultiplier", surgeMultiplier,
            "surgeActive", isSurge
        ));
    }

    /**
     * GET /api/flights/{id}/price-history – Returns simulated price history for the next N days.
     *
     * Feature 5 – Price History Graph.
     * Generates a price data point for each of the next {@code days} days so the
     * frontend can render a sparkline / bar chart showing how prices vary.
     *
     * @param id   flight ID
     * @param days number of future days to include (default 14, max 30)
     */
    @GetMapping("/{id}/price-history")
    public ResponseEntity<Map<String, Object>> getPriceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "14") int days) {

        if (days > 30) days = 30;
        Flight flight = flightService.getFlightById(id);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> history = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            LocalDateTime dt = date.atStartOfDay();
            double price = pricingService.calculatePrice(flight.getBasePrice(), dt);
            boolean surge = pricingService.isSurgeDay(date);
            history.add(Map.of(
                "date", date.format(fmt),
                "price", price,
                "surgeDay", surge
            ));
        }

        return ResponseEntity.ok(Map.of(
            "flightId", id,
            "basePrice", flight.getBasePrice(),
            "priceHistory", history
        ));
    }

    /** POST /api/flights – Creates a new flight (admin only) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Flight> createFlight(@RequestBody Flight flight) {
        return ResponseEntity.ok(flightService.saveFlight(flight));
    }

    /** DELETE /api/flights/{id} – Deletes a flight (admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}
