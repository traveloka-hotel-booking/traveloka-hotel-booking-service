package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.HotelRequestDto;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.dto.response.RestPage;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelListItemDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.entity.Booking;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.entity.enums.BookingStatus;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.util.HotelImageUtils;
import java.time.LocalDateTime;
import com.re.travelokahotelbookingservice.repository.BookingRepository;
import com.re.travelokahotelbookingservice.repository.HotelRepository;
import com.re.travelokahotelbookingservice.repository.PaymentRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.HotelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<HotelResponseDto> getAllHotels(String search, String city, Pageable pageable) {
        Page<Hotel> hotelsPage = hotelRepository.searchHotels(city, search, pageable);
        return hotelsPage.map(this::mapToResponseDto);
    }

    @Override
    @Cacheable(value = "hotels_list")
    @Transactional(readOnly = true)
    public List<HotelResponseDto> getAllHotelsList() {
        return hotelRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "hotel_detail", key = "#id")
    @Transactional(readOnly = true)
    public HotelResponseDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + id));
        return mapToResponseDtoWithRooms(hotel);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "hotel_detail", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
    @Transactional
    public HotelResponseDto createHotel(HotelRequestDto request) {
        String rawImages = request.getImages();
        String thumb = request.getThumbnailUrl();
        if ((thumb == null || thumb.isBlank()) && rawImages != null && !rawImages.isBlank()) {
            String delimiter = rawImages.contains(";;;") ? ";;;" : (rawImages.contains("|||") ? "\\|\\|\\|" : (rawImages.contains("\n") ? "\n" : ","));
            thumb = rawImages.split(delimiter)[0].trim();
        } else if ((rawImages == null || rawImages.isBlank()) && thumb != null && !thumb.isBlank()) {
            rawImages = thumb;
        }

        Hotel hotel = Hotel.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .starRating(request.getStarRating() != null ? request.getStarRating() : 5)
                .thumbnailUrl(thumb)
                .images(rawImages)
                .amenities(request.getAmenities())
                .createdAt(LocalDateTime.now())
                .build();

        Hotel saved = hotelRepository.save(hotel);

        log.info("Created new Hotel: id={}, name={}", saved.getId(), saved.getName());
        return mapToResponseDtoWithRooms(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotel_detail", key = "#id"),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
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
        if (request.getImages() != null) {
            hotel.setImages(request.getImages());
            if ((hotel.getThumbnailUrl() == null || hotel.getThumbnailUrl().isBlank()) && !request.getImages().isBlank()) {
                String delimiter = request.getImages().contains(";;;") ? ";;;" : (request.getImages().contains("|||") ? "\\|\\|\\|" : (request.getImages().contains("\n") ? "\n" : ","));
                hotel.setThumbnailUrl(request.getImages().split(delimiter)[0].trim());
            }
        }
        if (request.getAmenities() != null) {
            hotel.setAmenities(request.getAmenities());
        }

        if (request.getPricePerNight() != null && request.getPricePerNight().compareTo(BigDecimal.ZERO) > 0) {
            List<Room> rooms = roomRepository.findByHotelId(id);
            if (!rooms.isEmpty()) {
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
    @Caching(evict = {
            @CacheEvict(value = "hotel_detail", key = "#id"),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "hotel_rooms", allEntries = true),
            @CacheEvict(value = "room_detail", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách sạn với ID: " + id));

        List<Room> rooms = roomRepository.findByHotelId(id);

        // 1. Kiểm tra toàn bộ các phòng xem có khách đang ở hoặc có đơn đặt phòng đang hoạt động không
        for (Room r : rooms) {
            if (r.getStatus() != null) {
                if ("OCCUPIED".equalsIgnoreCase(r.getStatus())) {
                    throw new BusinessException("Không thể xóa khách sạn vì phòng '" + r.getName() + "' đang có khách lưu trú.");
                }
                if ("BOOKED".equalsIgnoreCase(r.getStatus())) {
                    throw new BusinessException("Không thể xóa khách sạn vì phòng '" + r.getName() + "' đang có khách đặt phòng.");
                }
            }
            List<Booking> activeBookings = bookingRepository.findActiveBookingsByRoomId(r.getId());
            if (!activeBookings.isEmpty()) {
                boolean hasCheckedIn = activeBookings.stream()
                        .anyMatch(b -> BookingStatus.CHECKED_IN.equals(b.getStatus()));
                if (hasCheckedIn) {
                    throw new BusinessException("Không thể xóa khách sạn vì phòng '" + r.getName() + "' đang có khách lưu trú (Check-in).");
                } else {
                    throw new BusinessException("Không thể xóa khách sạn vì phòng '" + r.getName() + "' đang có đơn đặt phòng hoạt động.");
                }
            }
        }

        // 2. Dọn dẹp đơn đặt phòng lịch sử (COMPLETED, CANCELLED, EXPIRED) và thanh toán liên quan trước khi xóa phòng
        for (Room r : rooms) {
            List<Booking> historicalBookings = bookingRepository.findByRoomId(r.getId());
            for (Booking b : historicalBookings) {
                if (b.getPayment() != null) {
                    paymentRepository.delete(b.getPayment());
                }
                bookingRepository.delete(b);
            }
            roomRepository.delete(r);
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
                .images(HotelImageUtils.extractImageList(hotel))
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
                        .amenities(r.getAmenities())
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
                .images(HotelImageUtils.extractImageList(hotel))
                .amenities(hotel.getAmenities())
                .minPrice(minPrice)
                .totalRooms(rooms.size())
                .createdAt(hotel.getCreatedAt())
                .rooms(roomDtos)
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotel_detail", key = "#id"),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true)
    })
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
    @Cacheable(
            value = "search_hotels",
            key = "(#city != null ? #city.toLowerCase().trim() : 'ALL') + '_' + #checkIn + '_' + #checkOut + '_' + #guests + '_' + (#pageable != null ? #pageable.pageNumber : 0) + '_' + (#pageable != null ? #pageable.pageSize : 10)"
    )
    public Page<HotelSearchResponseDto> searchHotels(
            String city,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests,
            Pageable pageable
    ) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        }
        if (checkIn == null) {
            checkIn = LocalDate.now();
        }
        if (checkOut == null) {
            checkOut = checkIn.plusDays(1);
        }
        if (guests == null || guests < 1) {
            guests = 1;
        }

        // 1. Build Specification lọc theo city
        Specification<Hotel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (city != null && !city.isBlank()) {
                String searchCity = normalizeCityKeyword(city);
                if (!searchCity.equalsIgnoreCase("Việt Nam")
                        && !searchCity.equalsIgnoreCase("Toàn bộ")
                        && !searchCity.equalsIgnoreCase("Tất cả")) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("city")),
                                    "%" + searchCity.toLowerCase() + "%"
                            )
                    );
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Hotel> hotelPage = hotelRepository.findAll(spec, pageable);
        List<Hotel> hotels = hotelPage.getContent();

        if (hotels.isEmpty()) {
            return new RestPage<>(Collections.emptyList(), pageable, 0);
        }

        // 2. Lấy danh sách ID các khách sạn trong trang hiện tại
        List<Long> hotelIds = hotels.stream().map(Hotel::getId).toList();

        // 3. Triệt tiêu N+1 Query: Batch query tìm tất cả phòng trống cho các khách sạn trong 1 câu SQL duy nhất
        List<Room> allAvailableRooms = roomRepository.findAvailableRoomsByHotelIds(
                hotelIds,
                guests,
                checkIn,
                checkOut
        );

        // Gom nhóm danh sách phòng theo hotelId trong bộ nhớ RAM
        Map<Long, List<Room>> roomsByHotelId = allAvailableRooms.stream()
                .collect(Collectors.groupingBy(r -> r.getHotel().getId()));

        // 4. Map sang DTO, chỉ giữ hotel có phòng còn trống
        List<HotelSearchResponseDto> content = hotels.stream()
                .map(hotel -> {
                    List<Room> availableRooms = roomsByHotelId.getOrDefault(hotel.getId(), Collections.emptyList());

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
                            .images(HotelImageUtils.extractImageList(hotel))
                            .minPricePerNight(minPrice)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return new RestPage<>(content, pageable, content.size());
    }

    //FR-07
    @Override
    @Cacheable(
            value = "filter_hotels",
            key = "(#filter != null && #filter.city != null ? #filter.city.toLowerCase().trim() : 'ALL') + '_' + (#filter != null && #filter.minStar != null ? #filter.minStar : 0) + '_' + (#filter != null && #filter.maxStar != null ? #filter.maxStar : 5) + '_' + (#filter != null && #filter.amenities != null ? #filter.amenities.toString() : 'NONE') + '_' + (#pageable != null ? #pageable.pageNumber : 0) + '_' + (#pageable != null ? #pageable.pageSize : 10) + '_' + (#pageable != null && #pageable.sort != null ? #pageable.sort.toString() : 'DEFAULT')"
    )
    public Page<HotelListItemDto> filterHotels(HotelFilterRequest filter, Pageable pageable) {

        Specification<Hotel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc theo city
            if (filter.getCity() != null && !filter.getCity().isBlank()) {
                String searchCity = normalizeCityKeyword(filter.getCity());
                predicates.add(
                        cb.like(
                                cb.lower(root.get("city")),
                                "%" + searchCity.toLowerCase() + "%"
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
        List<Hotel> hotels = hotelPage.getContent();

        if (hotels.isEmpty()) {
            return new RestPage<>(Collections.emptyList(), pageable, 0);
        }

        // Triệt tiêu N+1 Query: Lấy giá phòng thấp nhất của tất cả khách sạn trong 1 câu SQL duy nhất
        List<Long> hotelIds = hotels.stream().map(Hotel::getId).toList();
        List<Object[]> minPriceRows = roomRepository.findMinPricesByHotelIds(hotelIds);
        Map<Long, BigDecimal> minPriceMap = minPriceRows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1],
                        (existing, replacement) -> existing
                ));

        List<HotelListItemDto> content = hotels.stream()
                .map(hotel -> HotelListItemDto.builder()
                        .id(hotel.getId())
                        .name(hotel.getName())
                        .city(hotel.getCity())
                        .address(hotel.getAddress())
                        .starRating(hotel.getStarRating())
                        .thumbnailUrl(hotel.getThumbnailUrl())
                        .images(HotelImageUtils.extractImageList(hotel))
                        .minPricePerNight(minPriceMap.get(hotel.getId()))
                        .build())
                .toList();

        return new RestPage<>(content, pageable, hotelPage.getTotalElements());
    }

    /**
     * Chuẩn hóa từ khóa thành phố để tương thích linh hoạt giữa người dùng nhập và CSDL
     * Ví dụ: "TP. Hồ Chí Minh", "Thành phố Hồ Chí Minh", "Sài Gòn", "Đảo Phú Quốc",...
     */
    private String normalizeCityKeyword(String city) {
        if (city == null || city.isBlank()) {
            return "";
        }
        String clean = city.trim();
        String lower = clean.toLowerCase();
        if (lower.startsWith("tp.") || lower.startsWith("tp ")) {
            clean = clean.substring(clean.indexOf(' ') + 1).trim();
        } else if (lower.startsWith("thành phố ")) {
            clean = clean.substring("thành phố ".length()).trim();
        } else if (lower.startsWith("tỉnh ")) {
            clean = clean.substring("tỉnh ".length()).trim();
        } else if (lower.startsWith("đảo ")) {
            clean = clean.substring("đảo ".length()).trim();
        } else if (lower.equals("sài gòn") || lower.equals("sai gon") || lower.equals("hcm") || lower.equals("tphcm")) {
            clean = "Hồ Chí Minh";
        }
        return clean;
    }

}

