package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.RoomRequestDto;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.entity.Booking;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.entity.enums.BookingStatus;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.repository.BookingRepository;
import com.re.travelokahotelbookingservice.repository.HotelRepository;
import com.re.travelokahotelbookingservice.repository.PaymentRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.RoomService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;


    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> getAllRooms(Long hotelId, String status) {
        List<Room> rooms;
        if (hotelId != null) {
            if (status != null && !status.isBlank()) {
                rooms = roomRepository.findByHotelIdAndStatusWithHotel(hotelId, status.toUpperCase());
            } else {
                rooms = roomRepository.findByHotelIdWithHotel(hotelId);
            }
        } else if (status != null && !status.isBlank()) {
            rooms = roomRepository.findByStatusWithHotel(status.toUpperCase());
        } else {
            rooms = roomRepository.findAllWithHotel();
        }

        return rooms.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "hotel_rooms", key = "#hotelId")
    @Transactional(readOnly = true)
    public List<RoomResponseDto> getRoomsByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "room_detail", key = "#id")
    @Transactional(readOnly = true)
    public RoomResponseDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng với ID: " + id));
        return mapToDto(room);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotel_rooms", key = "#result.hotelId"),
            @CacheEvict(value = "hotel_detail", key = "#result.hotelId"),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
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
                .amenities(request.getAmenities())
                .build();

        Room saved = roomRepository.save(room);
        log.info("Created new Room id={}, name={} for Hotel id={}", saved.getId(), saved.getName(), hotel.getId());
        return mapToDto(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "room_detail", key = "#id"),
            @CacheEvict(value = "hotel_rooms", key = "#result.hotelId"),
            @CacheEvict(value = "hotel_detail", key = "#result.hotelId"),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
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
        if (request.getAmenities() != null) {
            room.setAmenities(request.getAmenities());
        }

        Room updated = roomRepository.save(room);
        log.info("Updated Room id={}, name={}", updated.getId(), updated.getName());
        return mapToDto(updated);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "room_detail", key = "#id"),
            @CacheEvict(value = "hotel_rooms", allEntries = true),
            @CacheEvict(value = "hotel_detail", allEntries = true),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng với ID: " + id));

        // 1. Kiểm tra trạng thái phòng: Nếu đang có khách ở hoặc đang được đặt -> Chặn không cho xóa
        if (room.getStatus() != null) {
            if ("OCCUPIED".equalsIgnoreCase(room.getStatus())) {
                throw new BusinessException("Không thể xóa loại phòng '" + room.getName() + "' vì đang có khách lưu trú.");
            }
            if ("BOOKED".equalsIgnoreCase(room.getStatus())) {
                throw new BusinessException("Không thể xóa loại phòng '" + room.getName() + "' vì đang có khách đặt phòng.");
            }
        }

        // 2. Kiểm tra các đơn đặt phòng đang hoạt động (PENDING, CONFIRMED, CHECKED_IN)
        List<Booking> activeBookings = bookingRepository.findActiveBookingsByRoomId(id);
        if (!activeBookings.isEmpty()) {
            boolean hasCheckedIn = activeBookings.stream()
                    .anyMatch(b -> BookingStatus.CHECKED_IN.equals(b.getStatus()));
            if (hasCheckedIn) {
                throw new BusinessException("Không thể xóa phòng '" + room.getName() + "' vì đang có khách lưu trú (Check-in).");
            } else {
                throw new BusinessException("Không thể xóa phòng '" + room.getName() + "' vì có đơn đặt phòng đang hoạt động.");
            }
        }

        // 3. Dọn dẹp các đơn đặt phòng lịch sử (COMPLETED, CANCELLED, EXPIRED) và thanh toán liên quan để tránh vi phạm ràng buộc khóa ngoại
        List<Booking> historicalBookings = bookingRepository.findByRoomId(id);
        for (Booking b : historicalBookings) {
            if (b.getPayment() != null) {
                paymentRepository.delete(b.getPayment());
            }
            bookingRepository.delete(b);
        }

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
                .amenities(room.getAmenities())
                .createdAt(room.getCreatedAt())
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "room_detail", key = "#id"),
            @CacheEvict(value = "hotel_rooms", key = "#result.hotelId"),
            @CacheEvict(value = "hotel_detail", key = "#result.hotelId")
    })
    @Transactional
    public RoomResponseDto updateRoomImage(Long id, MultipartFile file) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại phòng với ID: " + id));

        Map uploadResult = cloudinaryService.uploadImage(file, "traveloka/rooms", "room-" + id);
        String secureUrl = (String) uploadResult.get("secure_url");

        room.setImageUrl(secureUrl);
        Room saved = roomRepository.save(room);
        log.info("Updated image for Room id={}: {}", id, secureUrl);
        return mapToDto(saved);
    }

    @Override
    public String uploadRoomImage(MultipartFile file) {
        Map uploadResult = cloudinaryService.uploadImage(file, "traveloka/rooms", "room-temp");
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Uploaded room image to Cloudinary: {}", secureUrl);
        return secureUrl;
    }
}

