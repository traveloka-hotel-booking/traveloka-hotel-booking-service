package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.RoomRequestDto;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;

import java.util.List;

public interface RoomService {
    List<RoomResponseDto> getAllRooms(Long hotelId, String status);
    List<RoomResponseDto> getRoomsByHotelId(Long hotelId);
    RoomResponseDto getRoomById(Long id);
    RoomResponseDto createRoom(RoomRequestDto request);
    RoomResponseDto updateRoom(Long id, RoomRequestDto request);
    void deleteRoom(Long id);
}
