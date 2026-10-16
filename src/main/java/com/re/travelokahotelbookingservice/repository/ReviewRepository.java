package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

    boolean existsByBookingId(Long bookingId);

    Optional<Review> findByBookingId(Long bookingId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.hotel.id = :hotelId")
    Double calculateAverageRatingByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.hotel.id = :hotelId")
    Integer countByHotelId(@Param("hotelId") Long hotelId);
}
