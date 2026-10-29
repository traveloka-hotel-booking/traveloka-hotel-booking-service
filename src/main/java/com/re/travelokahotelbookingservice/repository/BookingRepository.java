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
                (b.checkIn < :checkOut AND b.checkOut > :checkIn)
              )
    """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.room r
        JOIN FETCH r.hotel h
        LEFT JOIN FETCH b.payment p
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findAllWithDetails();

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.room r
        JOIN FETCH r.hotel h
        LEFT JOIN FETCH b.payment p
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.room r
        JOIN FETCH r.hotel h
        LEFT JOIN FETCH b.payment p
        WHERE b.id = :id
    """)
    java.util.Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatusOrderByCreatedAtDesc(com.re.travelokahotelbookingservice.entity.enums.BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.room.id = :roomId
          AND b.status IN (
              com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING,
              com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED,
              com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN
          )
    """)
    List<Booking> findActiveBookingsByRoomId(@Param("roomId") Long roomId);

    List<Booking> findByRoomId(Long roomId);
}
