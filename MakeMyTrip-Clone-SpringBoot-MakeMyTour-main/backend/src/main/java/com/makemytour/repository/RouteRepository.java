package com.makemytour.repository;

import com.makemytour.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for Route entities.
 * Feature 2 – Interactive Route Planning.
 */
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /** Returns all routes belonging to a specific user */
    List<Route> findByUserId(Long userId);
}
