package com.re.travelokahotelbookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.re.travelokahotelbookingservice.entity.Room;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelId(Long hotelId);
    List<Room> findByHotelIdAndStatus(Long hotelId, String status);
    List<Room> findByStatus(String status);

    // FR-06: tìm phòng còn trống theo khoảng thời gian + số khách
    @Query("""
        SELECT r FROM Room r
        WHERE r.hotel.id = :hotelId
          AND r.maxGuests >= :guests
          AND r.status = 'AVAILABLE'
          AND r.id NOT IN (
                SELECT b.room.id FROM Booking b
                WHERE b.status IN (
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING,
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED,
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN
                )
                AND (b.checkIn < :checkOut AND b.checkOut > :checkIn)
          )
    """)
    List<Room> findAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("guests") Integer guests,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // FR-06: tìm phòng còn trống cho danh sách các khách sạn theo khoảng thời gian + số khách (Triệt tiêu N+1 Query)
    @Query("""
        SELECT r FROM Room r
        WHERE r.hotel.id IN :hotelIds
          AND r.maxGuests >= :guests
          AND r.status = 'AVAILABLE'
          AND r.id NOT IN (
                SELECT b.room.id FROM Booking b
                WHERE b.status IN (
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING,
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED,
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN
                )
                AND (b.checkIn < :checkOut AND b.checkOut > :checkIn)
          )
    """)
    List<Room> findAvailableRoomsByHotelIds(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("guests") Integer guests,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    //FR-07
    @Query("""
    SELECT MIN(r.pricePerNight)
    FROM Room r
    WHERE r.hotel.id = :hotelId
""")


    BigDecimal findMinPriceByHotelId(@Param("hotelId") Long hotelId);
}
