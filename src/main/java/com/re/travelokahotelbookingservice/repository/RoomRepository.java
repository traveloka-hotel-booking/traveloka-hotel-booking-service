package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelId(Long hotelId);
    List<Room> findByHotelIdAndStatus(Long hotelId, String status);
    List<Room> findByStatus(String status);
}
