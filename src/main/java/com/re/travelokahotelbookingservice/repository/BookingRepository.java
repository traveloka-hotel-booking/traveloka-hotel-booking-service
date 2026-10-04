package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.Booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.room.id = :roomId
          AND b.status IN (
              com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING,
              com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED,
              com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN
          )
          AND (
                (b.checkIn <= :checkOut AND b.checkOut >= :checkIn)
              )
    """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatusOrderByCreatedAtDesc(com.re.travelokahotelbookingservice.entity.enums.BookingStatus status);
}
