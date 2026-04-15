package com.makemytour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores a planned journey route as an ordered list of Waypoints.
 * Supports traffic-aware ETA estimation via the Haversine formula.
 *
 * Feature 2 – Interactive Route Planning.
 */
@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name for the route (e.g., "Mumbai → Goa Journey") */
    @Column(nullable = false)
    private String name;

    /** Owner of this route (nullable for public/shared routes) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Ordered collection of waypoints.
     * Stored in a join table "route_waypoints" ordered by the waypoint's sequence field.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_waypoints",
                     joinColumns = @JoinColumn(name = "route_id"))
    @OrderColumn(name = "waypoint_order")
    @Builder.Default
    private List<Waypoint> waypoints = new ArrayList<>();

    /** Estimated time of arrival (minutes) calculated from base distance */
    private Integer baseEtaMinutes;

    /**
     * Traffic multiplier applied at query time (e.g., 1.3 = 30% extra travel time).
     * Stored so clients can compare ETA across different traffic conditions.
     */
    @Builder.Default
    private Double trafficMultiplier = 1.0;

    /** Total distance in kilometres (Haversine-computed) */
    private Double distanceKm;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
