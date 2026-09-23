package com.re.travelokahotelbookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.re.travelokahotelbookingservice.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
