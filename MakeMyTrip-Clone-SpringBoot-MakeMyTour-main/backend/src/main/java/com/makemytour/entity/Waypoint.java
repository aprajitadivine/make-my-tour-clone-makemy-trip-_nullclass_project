package com.makemytour.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single geographic point on a journey route.
 * Stored as a collection element within the Route entity.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waypoint {

    /** Human-readable label (e.g., "Mumbai Airport", "Pune Bus Stand") */
    private String name;

    /** Latitude in decimal degrees (e.g., 19.0896) */
    private Double latitude;

    /** Longitude in decimal degrees (e.g., 72.8656) */
    private Double longitude;

    /** Sequence order of this waypoint along the route */
    private Integer sequence;
}
