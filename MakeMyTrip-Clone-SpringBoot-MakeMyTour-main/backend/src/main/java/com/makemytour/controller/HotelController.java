package com.makemytour.controller;

import com.makemytour.entity.Hotel;
import com.makemytour.service.DynamicPricingService;
import com.makemytour.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST API for hotel search and dynamic pricing.
 */
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final DynamicPricingService pricingService;

    /** GET /api/hotels – Lists all hotels */
    @GetMapping
    public ResponseEntity<List<Hotel>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    /** GET /api/hotels/{id} – Returns a single hotel */
    @GetMapping("/{id}")
    public ResponseEntity<Hotel> getHotelById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    /** GET /api/hotels/search?city=Goa – Searches hotels by city */
    @GetMapping("/search")
    public ResponseEntity<List<Hotel>> searchHotels(
            @RequestParam String city,
            @RequestParam(required = false, defaultValue = "1") int minStars) {
        return ResponseEntity.ok(hotelService.searchHotelsByCityAndRating(city, minStars));
    }

    /**
     * GET /api/hotels/{id}/pricing – Dynamic price for check-in date (Feature 5).
     */
    @GetMapping("/{id}/pricing")
    public ResponseEntity<Map<String, Object>> getDynamicPrice(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInDate) {

        Hotel hotel = hotelService.getHotelById(id);
        double dynamicPrice = pricingService.calculatePrice(hotel.getBasePrice(), checkInDate);
        double surgeMultiplier = pricingService.getSurgeMultiplier(checkInDate);

        return ResponseEntity.ok(Map.of(
            "hotelId", id,
            "basePrice", hotel.getBasePrice(),
            "dynamicPrice", dynamicPrice,
            "surgeMultiplier", surgeMultiplier,
            "surgeActive", surgeMultiplier > 1.0
        ));
    }

    /**
     * GET /api/hotels/{id}/price-history – Returns simulated price history for the next N days.
     *
     * Feature 5 – Price History Graph.
     *
     * @param id   hotel ID
     * @param days number of future days to include (default 14, max 30)
     */
    @GetMapping("/{id}/price-history")
    public ResponseEntity<Map<String, Object>> getPriceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "14") int days) {

        if (days > 30) days = 30;
        Hotel hotel = hotelService.getHotelById(id);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> history = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            LocalDateTime dt = date.atStartOfDay();
            double price = pricingService.calculatePrice(hotel.getBasePrice(), dt);
            boolean surge = pricingService.isSurgeDay(date);
            history.add(Map.of(
                "date", date.format(fmt),
                "price", price,
                "surgeDay", surge
            ));
        }

        return ResponseEntity.ok(Map.of(
            "hotelId", id,
            "basePrice", hotel.getBasePrice(),
            "priceHistory", history
        ));
    }

    /** POST /api/hotels – Creates a hotel (admin only) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hotel> createHotel(@RequestBody Hotel hotel) {
        return ResponseEntity.ok(hotelService.saveHotel(hotel));
    }

    /** DELETE /api/hotels/{id} – Deletes a hotel (admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
