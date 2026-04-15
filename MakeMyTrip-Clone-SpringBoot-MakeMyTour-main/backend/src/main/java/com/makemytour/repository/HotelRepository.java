package com.makemytour.repository;

import com.makemytour.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByCityIgnoreCase(String city);

    List<Hotel> findByCityIgnoreCaseAndStarRatingGreaterThanEqual(String city, int minStars);
}
