package com.makemytour.service;

import com.makemytour.entity.Hotel;
import com.makemytour.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * CRUD and search operations for Hotel entities.
 */
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;

    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    public Hotel getHotelById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Hotel not found: " + id));
    }

    public List<Hotel> searchHotelsByCity(String city) {
        return hotelRepository.findByCityIgnoreCase(city);
    }

    public List<Hotel> searchHotelsByCityAndRating(String city, int minStars) {
        return hotelRepository.findByCityIgnoreCaseAndStarRatingGreaterThanEqual(city, minStars);
    }

    public Hotel saveHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    public void deleteHotel(Long id) {
        hotelRepository.deleteById(id);
    }
}
