package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.RoomRequestDto;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.repository.HotelRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.service.RoomService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> getAllRooms(Long hotelId, String status) {
        List<Room> rooms;
        if (hotelId != null) {
            if (status != null && !status.isBlank()) {
                rooms = roomRepository.findByHotelIdAndStatus(hotelId, status.toUpperCase());
            } else {
                rooms = roomRepository.findByHotelId(hotelId);
            }
        } else if (status != null && !status.isBlank()) {
            rooms = roomRepository.findByStatus(status.toUpperCase());
        } else {
            rooms = roomRepository.findAll();
        }

        return rooms.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> getRoomsByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng với ID: " + id));
        return mapToDto(room);
    }

    @Override
    @Transactional
    public RoomResponseDto createRoom(RoomRequestDto request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + request.getHotelId()));

        Room room = Room.builder()
                .hotel(hotel)
                .name(request.getName().trim())
                .description(request.getDescription())
                .maxGuests(request.getMaxGuests())
                .pricePerNight(request.getPricePerNight())
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().toUpperCase() : "AVAILABLE")
                .imageUrl(request.getImageUrl())
                .bedType(request.getBedType())
                .roomSize(request.getRoomSize())
                .build();

        Room saved = roomRepository.save(room);
        log.info("Created new Room id={}, name={} for Hotel id={}", saved.getId(), saved.getName(), hotel.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public RoomResponseDto updateRoom(Long id, RoomRequestDto request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng với ID: " + id));

        if (request.getHotelId() != null && !request.getHotelId().equals(room.getHotel().getId())) {
            Hotel newHotel = hotelRepository.findById(request.getHotelId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + request.getHotelId()));
            room.setHotel(newHotel);
        }

        room.setName(request.getName().trim());
        room.setDescription(request.getDescription());
        room.setMaxGuests(request.getMaxGuests());
        room.setPricePerNight(request.getPricePerNight());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            room.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getImageUrl() != null) {
            room.setImageUrl(request.getImageUrl());
        }
        if (request.getBedType() != null) {
            room.setBedType(request.getBedType());
        }
        if (request.getRoomSize() != null) {
            room.setRoomSize(request.getRoomSize());
        }

        Room updated = roomRepository.save(room);
        log.info("Updated Room id={}, name={}", updated.getId(), updated.getName());
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng với ID: " + id));
        roomRepository.delete(room);
        log.info("Deleted Room id={}", id);
    }

    private RoomResponseDto mapToDto(Room room) {
        return RoomResponseDto.builder()
                .id(room.getId())
                .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
                .hotelName(room.getHotel() != null ? room.getHotel().getName() : null)
                .name(room.getName())
                .description(room.getDescription())
                .maxGuests(room.getMaxGuests())
                .pricePerNight(room.getPricePerNight())
                .status(room.getStatus())
                .imageUrl(room.getImageUrl())
                .bedType(room.getBedType())
                .roomSize(room.getRoomSize())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
