package com.makemytour.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight health-check endpoint.
 *
 * <p>GET /api/health returns {@code {"status":"UP"}} without touching the database.
 * This endpoint is used by:
 * <ul>
 *   <li>Render's {@code healthCheckPath} to verify the service is alive after a cold start.</li>
 *   <li>External uptime monitors (e.g. UptimeRobot, cron-job.org) that ping the backend
 *       every ~14 minutes to prevent the free-tier service from spinning down.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /** GET /api/health — returns 200 OK without a database query */
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
