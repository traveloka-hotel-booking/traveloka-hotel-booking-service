package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.dto.hotel.HotelListItemDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.repository.HotelRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.service.HotelService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelServiceImpl(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    // FR-06: Search Hotels
    @Override
    public Page<HotelSearchResponseDto> searchHotels(
            String city,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests
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

        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());

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
                for (String amenity : filter.getAmenities()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("amenities")),
                                    "%" + amenity.toLowerCase() + "%"
                            )
                    );
                }
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
                            .starRating(hotel.getStarRating())
                            .thumbnailUrl(hotel.getThumbnailUrl())
                            .minPricePerNight(minPrice)
                            .build();
                })
                .toList();

        return new PageImpl<>(content, pageable, hotelPage.getTotalElements());
    }

}