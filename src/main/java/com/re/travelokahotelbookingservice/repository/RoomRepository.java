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

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel h ORDER BY r.id DESC")
    List<Room> findAllWithHotel();

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel h WHERE r.hotel.id = :hotelId ORDER BY r.id DESC")
    List<Room> findByHotelIdWithHotel(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel h WHERE r.status = :status ORDER BY r.id DESC")
    List<Room> findByStatusWithHotel(@Param("status") String status);

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel h WHERE r.hotel.id = :hotelId AND r.status = :status ORDER BY r.id DESC")
    List<Room> findByHotelIdAndStatusWithHotel(@Param("hotelId") Long hotelId, @Param("status") String status);

    List<Room> findByHotelId(Long hotelId);
    List<Room> findByHotelIdAndStatus(Long hotelId, String status);
    List<Room> findByStatus(String status);

    // FR-06: Tìm phòng còn trống theo danh sách hotelIds, số khách và khoảng thời gian (Triệt tiêu N+1 Query)
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

    // FR-06: Wrapper tìm phòng còn trống cho 1 khách sạn đơn lẻ (tái sử dụng query trên, tránh lặp JPQL)
    default List<Room> findAvailableRooms(
            Long hotelId,
            Integer guests,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        if (hotelId == null) {
            return List.of();
        }
        return findAvailableRoomsByHotelIds(List.of(hotelId), guests, checkIn, checkOut);
    }

    // FR-07: Tìm giá phòng thấp nhất cho danh sách các khách sạn (Triệt tiêu N+1 Query)
    @Query("""
        SELECT r.hotel.id, MIN(r.pricePerNight)
        FROM Room r
        WHERE r.hotel.id IN :hotelIds
        GROUP BY r.hotel.id
    """)
    List<Object[]> findMinPricesByHotelIds(@Param("hotelIds") List<Long> hotelIds);

    // FR-07: Wrapper tìm giá thấp nhất cho 1 khách sạn đơn lẻ (tái sử dụng query trên, tránh lặp JPQL)
    default BigDecimal findMinPriceByHotelId(Long hotelId) {
        if (hotelId == null) {
            return null;
        }
        List<Object[]> results = findMinPricesByHotelIds(List.of(hotelId));
        if (results != null && !results.isEmpty() && results.get(0)[1] != null) {
            return (BigDecimal) results.get(0)[1];
        }
        return null;
    }
}
