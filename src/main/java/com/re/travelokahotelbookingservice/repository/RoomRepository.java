package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;


public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotel_Id(Long hotelId);

    // FR-06: tìm phòng còn trống theo khoảng thời gian + số khách
    @Query("""
        SELECT r FROM Room r
        WHERE r.hotel.id = :hotelId
          AND r.maxGuests >= :guests
          AND r.status = 'AVAILABLE'
          AND r.id NOT IN (
                SELECT b.room.id FROM Booking b
                WHERE NOT (b.checkOut <= :checkIn OR b.checkIn >= :checkOut)
          )
    """)
    List<Room> findAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("guests") Integer guests,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    BigDecimal findMinPriceByHotelId(@Param("hotelId") Long hotelId);
}