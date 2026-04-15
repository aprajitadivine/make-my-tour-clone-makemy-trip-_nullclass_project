package com.makemytour.repository;

import com.makemytour.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    /** Search flights by origin, destination, and departure date window (case-insensitive) */
    @Query("SELECT f FROM Flight f WHERE LOWER(f.origin) = LOWER(:origin) AND LOWER(f.destination) = LOWER(:destination) " +
           "AND f.departureTime BETWEEN :start AND :end")
    List<Flight> searchFlights(String origin, String destination,
                               LocalDateTime start, LocalDateTime end);

    List<Flight> findByOriginAndDestination(String origin, String destination);
}
