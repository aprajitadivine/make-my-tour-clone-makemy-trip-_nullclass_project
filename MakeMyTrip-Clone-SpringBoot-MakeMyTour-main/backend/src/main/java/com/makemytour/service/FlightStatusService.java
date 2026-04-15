package com.makemytour.service;

import com.makemytour.entity.Flight;
import com.makemytour.enums.FlightStatus;
import com.makemytour.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Feature 3 – Live Status Mock
 *
 * Simulates a real-time flight status board by randomly toggling a flight's
 * status between ON_TIME, DELAYED, and CANCELLED.
 *
 * When a flight is DELAYED the service also generates:
 *  - delayMinutes  : how many minutes the flight is delayed (15–120 min)
 *  - delayReason   : a realistic, human-readable cause of the delay
 *
 * In production this would be replaced by an airline API integration
 * (e.g. OAG, FlightAware, AviationStack).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightStatusService {

    private final FlightRepository flightRepository;

    // Weighted distribution: 60% ON_TIME, 30% DELAYED, 10% CANCELLED
    private static final FlightStatus[] WEIGHTED_STATUSES = {
        FlightStatus.ON_TIME, FlightStatus.ON_TIME, FlightStatus.ON_TIME,
        FlightStatus.ON_TIME, FlightStatus.ON_TIME, FlightStatus.ON_TIME,
        FlightStatus.DELAYED, FlightStatus.DELAYED, FlightStatus.DELAYED,
        FlightStatus.CANCELLED
    };

    /** Realistic delay reasons shown to passengers (Feature 3) */
    private static final List<String> DELAY_REASONS = List.of(
        "Air traffic congestion",
        "Late arrival of incoming aircraft",
        "Adverse weather conditions",
        "Technical/maintenance check",
        "Crew scheduling issue",
        "Airport ground operations delay",
        "Security screening delay",
        "Waiting for connecting passengers",
        "Refuelling delay",
        "Baggage loading overrun"
    );

    private final Random random = new Random();

    /**
     * Randomly assigns a new status to the specified flight.
     * If DELAYED, also assigns delay minutes (15–120) and a reason.
     *
     * @param flightId the ID of the flight to update
     * @return the updated Flight entity
     */
    @Transactional
    public Flight updateFlightStatus(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new NoSuchElementException("Flight not found: " + flightId));

        FlightStatus previousStatus = flight.getStatus();
        FlightStatus newStatus = WEIGHTED_STATUSES[random.nextInt(WEIGHTED_STATUSES.length)];
        flight.setStatus(newStatus);

        if (newStatus == FlightStatus.DELAYED) {
            // Delay in multiples of 15 min, between 15 and 120 minutes
            int delayMins = (random.nextInt(8) + 1) * 15;
            flight.setDelayMinutes(delayMins);
            flight.setDelayReason(DELAY_REASONS.get(random.nextInt(DELAY_REASONS.size())));
        } else {
            flight.setDelayMinutes(0);
            flight.setDelayReason(null);
        }

        log.info("Flight {} status: {} → {} (delay: {} min, reason: {})",
                flight.getFlightNumber(), previousStatus, newStatus,
                flight.getDelayMinutes(), flight.getDelayReason());

        return flightRepository.save(flight);
    }

    /**
     * Returns the current status of a flight without modifying it.
     */
    public FlightStatus getFlightStatus(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new NoSuchElementException("Flight not found: " + flightId));
        return flight.getStatus();
    }

    /**
     * Bulk-refreshes the status of all flights in the system.
     */
    @Transactional
    public void refreshAllFlightStatuses() {
        flightRepository.findAll().forEach(flight -> updateFlightStatus(flight.getId()));
        log.info("Refreshed statuses for all flights");
    }
}
