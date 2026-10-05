package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.HotelRequestDto;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelListItemDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.repository.HotelRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.HotelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final CloudinaryService cloudinaryService;


    @Override
    @Transactional(readOnly = true)
    public Page<HotelResponseDto> getAllHotels(String search, String city, Pageable pageable) {
        Page<Hotel> hotelsPage = hotelRepository.searchHotels(city, search, pageable);
        return hotelsPage.map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponseDto> getAllHotelsList() {
        return hotelRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelResponseDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + id));
        return mapToResponseDtoWithRooms(hotel);
    }

    @Override
    @Transactional
    public HotelResponseDto createHotel(HotelRequestDto request) {
        Hotel hotel = Hotel.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .starRating(request.getStarRating() != null ? request.getStarRating() : 5)
                .thumbnailUrl(request.getThumbnailUrl())
                .amenities(request.getAmenities())
                .build();

        Hotel saved = hotelRepository.save(hotel);

        if (request.getPricePerNight() != null && request.getPricePerNight().compareTo(BigDecimal.ZERO) > 0) {
            Room defaultRoom = Room.builder()
                    .hotel(saved)
                    .name("Phòng Tiêu Chuẩn - " + saved.getName())
                    .maxGuests(2)
                    .pricePerNight(request.getPricePerNight())
                    .status("AVAILABLE")
                    .build();
            roomRepository.save(defaultRoom);
        }

        log.info("Created new Hotel: id={}, name={}", saved.getId(), saved.getName());
        return mapToResponseDtoWithRooms(saved);
    }

    @Override
    @Transactional
    public HotelResponseDto updateHotel(Long id, HotelRequestDto request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + id));

        hotel.setName(request.getName().trim());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress().trim());
        hotel.setCity(request.getCity().trim());
        if (request.getStarRating() != null) {
            hotel.setStarRating(request.getStarRating());
        }
        if (request.getThumbnailUrl() != null) {
            hotel.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getAmenities() != null) {
            hotel.setAmenities(request.getAmenities());
        }

        if (request.getPricePerNight() != null && request.getPricePerNight().compareTo(BigDecimal.ZERO) > 0) {
            List<Room> rooms = roomRepository.findByHotelId(id);
            if (rooms.isEmpty()) {
                Room defaultRoom = Room.builder()
                        .hotel(hotel)
                        .name("Phòng Tiêu Chuẩn - " + hotel.getName())
                        .maxGuests(2)
                        .pricePerNight(request.getPricePerNight())
                        .status("AVAILABLE")
                        .build();
                roomRepository.save(defaultRoom);
            } else {
                // Cập nhật giá cho phòng có giá thấp nhất (giá khởi điểm của khách sạn)
                Room targetRoom = rooms.stream()
                        .min(java.util.Comparator.comparing(Room::getPricePerNight))
                        .orElse(rooms.get(0));
                targetRoom.setPricePerNight(request.getPricePerNight());
                roomRepository.save(targetRoom);
            }
        }

        Hotel updated = hotelRepository.save(hotel);
        log.info("Updated Hotel: id={}, name={}", updated.getId(), updated.getName());
        return mapToResponseDtoWithRooms(updated);
    }

    @Override
    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + id));

        List<Room> rooms = roomRepository.findByHotelId(id);
        if (!rooms.isEmpty()) {
            roomRepository.deleteAll(rooms);
        }

        hotelRepository.delete(hotel);
        log.info("Deleted Hotel id={}", id);
    }

    private HotelResponseDto mapToResponseDto(Hotel hotel) {
        List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
        BigDecimal minPrice = rooms.stream()
                .map(Room::getPricePerNight)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return HotelResponseDto.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .starRating(hotel.getStarRating())
                .thumbnailUrl(hotel.getThumbnailUrl())
                .amenities(hotel.getAmenities())
                .minPrice(minPrice)
                .totalRooms(rooms.size())
                .createdAt(hotel.getCreatedAt())
                .rooms(Collections.emptyList())
                .build();
    }

    private HotelResponseDto mapToResponseDtoWithRooms(Hotel hotel) {
        List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
        BigDecimal minPrice = rooms.stream()
                .map(Room::getPricePerNight)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<RoomResponseDto> roomDtos = rooms.stream()
                .map(r -> RoomResponseDto.builder()
                        .id(r.getId())
                        .hotelId(hotel.getId())
                        .hotelName(hotel.getName())
                        .name(r.getName())
                        .description(r.getDescription())
                        .maxGuests(r.getMaxGuests())
                        .pricePerNight(r.getPricePerNight())
                        .status(r.getStatus())
                        .imageUrl(r.getImageUrl())
                        .bedType(r.getBedType())
                        .roomSize(r.getRoomSize())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return HotelResponseDto.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .starRating(hotel.getStarRating())
                .thumbnailUrl(hotel.getThumbnailUrl())
                .amenities(hotel.getAmenities())
                .minPrice(minPrice)
                .totalRooms(rooms.size())
                .createdAt(hotel.getCreatedAt())
                .rooms(roomDtos)
                .build();
    }

    @Override
    @Transactional
    public HotelResponseDto updateThumbnail(Long id, MultipartFile file) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + id));

        Map uploadResult = cloudinaryService.uploadImage(file, "traveloka/hotels", "hotel-" + id);
        String secureUrl = (String) uploadResult.get("secure_url");

        hotel.setThumbnailUrl(secureUrl);
        Hotel saved = hotelRepository.save(hotel);
        log.info("Updated thumbnail for Hotel id={}: {}", id, secureUrl);
        return mapToResponseDtoWithRooms(saved);
    }

    @Override
    public String uploadHotelImage(MultipartFile file) {
        Map uploadResult = cloudinaryService.uploadImage(file, "traveloka/hotels", "hotel-temp");
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Uploaded hotel image to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    // FR-06: Search Hotels
    @Override
    public Page<HotelSearchResponseDto> searchHotels(
            String city,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests,
            Pageable pageable
    ) {

        // 1. Build Specification lọc theo city
        Specification<Hotel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (city != null && !city.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("city")),
                                "%" + city.toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        }

        Page<Hotel> hotelPage = hotelRepository.findAll(spec, pageable);

        // 2. Map sang DTO, chỉ giữ hotel có phòng còn trống
        List<HotelSearchResponseDto> content = hotelPage.getContent().stream()
                .map(hotel -> {

                    // Dùng JPQL kiểm tra phòng còn trống đúng FR-06
                    List<Room> availableRooms = roomRepository.findAvailableRooms(
                            hotel.getId(),
                            guests,
                            checkIn,
                            checkOut
                    );

                    // Nếu không có phòng phù hợp → loại hotel
                    if (availableRooms.isEmpty()) {
                        return null;
                    }

                    // Tính minPricePerNight
                    BigDecimal minPrice = availableRooms.stream()
                            .map(Room::getPricePerNight)
                            .min(BigDecimal::compareTo)
                            .orElse(null);

                    return HotelSearchResponseDto.builder()
                            .id(hotel.getId())
                            .name(hotel.getName())
                            .city(hotel.getCity())
                            .address(hotel.getAddress())
                            .starRating(hotel.getStarRating())
                            .thumbnailUrl(hotel.getThumbnailUrl())
                            .minPricePerNight(minPrice)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, content.size());

    }

    //FR-07
    @Override
    public Page<HotelListItemDto> filterHotels(HotelFilterRequest filter, Pageable pageable) {

        Specification<Hotel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc theo city
            if (filter.getCity() != null && !filter.getCity().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("city")),
                                "%" + filter.getCity().toLowerCase() + "%"
                        )
                );
            }


            // Lọc theo amenities
            if (filter.getAmenities() != null && !filter.getAmenities().isEmpty()) {

                List<Predicate> amenityPredicates = new ArrayList<>();

                for (String amenity : filter.getAmenities()) {
                    amenityPredicates.add(
                            cb.like(
                                    cb.lower(root.get("amenities")),
                                    "%" + amenity.toLowerCase() + "%"
                            )
                    );
                }

                // OR thay vì AND
                predicates.add(cb.or(amenityPredicates.toArray(new Predicate[0])));
            }


            // Lọc theo số sao
            if (filter.getMinStar() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("starRating"), filter.getMinStar()));
            }

            if (filter.getMaxStar() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("starRating"), filter.getMaxStar()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Hotel> hotelPage = hotelRepository.findAll(spec, pageable);

        List<HotelListItemDto> content = hotelPage.getContent().stream()
                .map(hotel -> {

                    // Lấy minPrice từ RoomRepository
                    BigDecimal minPrice = roomRepository.findMinPriceByHotelId(hotel.getId());

                    return HotelListItemDto.builder()
                            .id(hotel.getId())
                            .name(hotel.getName())
                            .city(hotel.getCity())
                            .address(hotel.getAddress())
                            .starRating(hotel.getStarRating())
                            .thumbnailUrl(hotel.getThumbnailUrl())
                            .minPricePerNight(minPrice)
                            .build();
                })
                .toList();

        return new PageImpl<>(content, pageable, hotelPage.getTotalElements());
    }

}

