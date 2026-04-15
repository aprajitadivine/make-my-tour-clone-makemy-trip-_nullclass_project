package com.makemytour.dto;

import com.makemytour.entity.Waypoint;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body for creating or updating a Route.
 * Feature 2 – Interactive Route Planning.
 */
@Data
public class RouteRequest {

    @NotBlank(message = "Route name is required")
    private String name;

    @NotEmpty(message = "At least one waypoint is required")
    private List<Waypoint> waypoints;

    /** Optional traffic multiplier supplied by the client (e.g., from a Maps API) */
    private Double trafficMultiplier;
}
