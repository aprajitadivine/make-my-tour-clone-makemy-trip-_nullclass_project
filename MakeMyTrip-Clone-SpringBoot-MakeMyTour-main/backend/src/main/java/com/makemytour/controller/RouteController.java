package com.makemytour.controller;

import com.makemytour.dto.RouteRequest;
import com.makemytour.entity.Route;
import com.makemytour.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API for the Interactive Route Planning feature.
 * Feature 2 – Interactive Route Planning.
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /** POST /api/routes – Creates a new route with waypoints */
    @PostMapping
    public ResponseEntity<?> createRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RouteRequest request) {
        try {
            Route route = routeService.createRoute(userDetails.getUsername(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(route);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/routes/my – Returns all routes for the authenticated user */
    @GetMapping("/my")
    public ResponseEntity<List<Route>> getMyRoutes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(routeService.getMyRoutes(userDetails.getUsername()));
    }

    /** GET /api/routes/{id} – Returns a single route by id */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoute(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(routeService.getRouteById(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** DELETE /api/routes/{id} – Deletes a route owned by the current user */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            routeService.deleteRoute(userDetails.getUsername(), id);
            return ResponseEntity.ok(Map.of("message", "Route deleted"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
