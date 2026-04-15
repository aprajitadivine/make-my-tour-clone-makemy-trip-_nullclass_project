package com.makemytour.service;

import com.makemytour.dto.RouteRequest;
import com.makemytour.entity.Route;
import com.makemytour.entity.User;
import com.makemytour.entity.Waypoint;
import com.makemytour.repository.RouteRepository;
import com.makemytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Business logic for the Interactive Route Planning feature.
 *
 * Key responsibilities:
 *  - Persist routes with ordered waypoints
 *  - Calculate total distance via the Haversine formula
 *  - Estimate traffic-aware ETA (assumes average 60 km/h road speed)
 *
 * Feature 2 – Interactive Route Planning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {

    /** Assumed average road speed in km/h for ETA calculation */
    private static final double AVG_SPEED_KMH = 60.0;

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;

    /**
     * Creates and persists a new route with its waypoints.
     * Automatically calculates distance and base ETA.
     */
    @Transactional
    public Route createRoute(String username, RouteRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        double distanceKm = calculateTotalDistance(request.getWaypoints());
        double multiplier = request.getTrafficMultiplier() != null
                ? request.getTrafficMultiplier() : 1.0;
        int etaMinutes = (int) Math.ceil((distanceKm / AVG_SPEED_KMH) * 60 * multiplier);

        Route route = Route.builder()
                .name(request.getName())
                .user(user)
                .waypoints(request.getWaypoints())
                .distanceKm(distanceKm)
                .trafficMultiplier(multiplier)
                .baseEtaMinutes(etaMinutes)
                .build();

        Route saved = routeRepository.save(route);
        log.info("Route '{}' created for user {} (distance={} km, ETA={} min)",
                saved.getName(), username, distanceKm, etaMinutes);
        return saved;
    }

    /** Returns all routes for the authenticated user */
    public List<Route> getMyRoutes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return routeRepository.findByUserId(user.getId());
    }

    /** Returns a single route by id (public access) */
    public Route getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Route not found: " + id));
    }

    /** Deletes a route (only the owner may delete it) */
    @Transactional
    public void deleteRoute(String username, Long id) {
        Route route = getRouteById(id);
        if (route.getUser() == null || !route.getUser().getUsername().equals(username)) {
            throw new SecurityException("You can only delete your own routes");
        }
        routeRepository.deleteById(id);
    }

    /**
     * Calculates the total great-circle distance along all consecutive waypoints
     * using the Haversine formula.
     *
     * @param waypoints ordered list of waypoints
     * @return total distance in kilometres
     */
    public double calculateTotalDistance(List<Waypoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            total += haversineKm(waypoints.get(i), waypoints.get(i + 1));
        }
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Haversine formula: calculates the great-circle distance between two
     * geographic points given their latitude/longitude.
     */
    private double haversineKm(Waypoint a, Waypoint b) {
        final double R = 6371.0; // Earth's radius in km
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);
        double hav = sinDLat * sinDLat
                + Math.cos(Math.toRadians(a.getLatitude()))
                * Math.cos(Math.toRadians(b.getLatitude()))
                * sinDLon * sinDLon;
        return 2 * R * Math.asin(Math.sqrt(hav));
    }
}
