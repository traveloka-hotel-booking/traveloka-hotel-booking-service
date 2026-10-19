package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.hotel.HotelRequestDto;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.dto.response.RestPageResponse;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelListItemDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.entity.Booking;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.entity.enums.BookingStatus;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import java.time.LocalDateTime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import com.re.travelokahotelbookingservice.repository.BookingRepository;
import com.re.travelokahotelbookingservice.repository.HotelRepository;
import com.re.travelokahotelbookingservice.repository.PaymentRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.repository.ReviewRepository;
import com.re.travelokahotelbookingservice.entity.Review;
import java.time.format.DateTimeFormatter;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.HotelService;
import com.re.travelokahotelbookingservice.service.GoogleGeocodingService;
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
import java.util.HashMap;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> IMAGE_LIST_TYPE_REF = new TypeReference<>() {};

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final GoogleGeocodingService googleGeocodingService;


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

        String rawAddress = request.getAddress() != null ? request.getAddress().trim() : "";
        String standardizedAddress = (googleGeocodingService != null && !rawAddress.isBlank()) 
                ? googleGeocodingService.validateAddress(rawAddress) 
                : rawAddress;

        Hotel hotel = Hotel.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(standardizedAddress)
                .city(request.getCity().trim())
                .starRating(request.getStarRating() != null ? request.getStarRating() : 5)
                .pricePerNight(request.getPricePerNight())
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
        if (request.getAddress() != null) {
            String rawAddress = request.getAddress().trim();
            String standardizedAddress = (googleGeocodingService != null && !rawAddress.isBlank()) 
                    ? googleGeocodingService.validateAddress(rawAddress) 
                    : rawAddress;
            hotel.setAddress(standardizedAddress);
        }
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
            hotel.setPricePerNight(request.getPricePerNight());
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
        BigDecimal minPrice = (hotel.getPricePerNight() != null && hotel.getPricePerNight().compareTo(BigDecimal.ZERO) > 0)
                ? hotel.getPricePerNight()
                : rooms.stream()
                    .map(Room::getPricePerNight)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

        Double avgRating = (reviewRepository != null) ? reviewRepository.calculateAverageRatingByHotelId(hotel.getId()) : null;
        Integer count = (reviewRepository != null) ? reviewRepository.countByHotelId(hotel.getId()) : 0;
        if (count != null && count > 0 && avgRating != null && avgRating > 0) {
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        } else {
            avgRating = null;
            count = 0;
        }
        String ratingText = null;
        if (avgRating != null && count > 0) {
            ratingText = avgRating >= 9.0 ? "Xuất sắc" : (avgRating >= 8.0 ? "Tuyệt vời" : (avgRating >= 7.0 ? "Rất tốt" : "Hài lòng"));
        }

        String latestComment = null;
        if (reviewRepository != null && count > 0) {
            List<Review> latest = reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotel.getId());
            if (!latest.isEmpty() && latest.get(0).getComment() != null) {
                latestComment = latest.get(0).getComment();
            }
        }

        return HotelResponseDto.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .starRating(hotel.getStarRating())
                .thumbnailUrl(hotel.getThumbnailUrl())
                .images(extractImageList(hotel))
                .amenities(hotel.getAmenities())
                .minPrice(minPrice)
                .totalRooms(rooms.size())
                .createdAt(hotel.getCreatedAt())
                .rooms(Collections.emptyList())
                .averageRating(avgRating)
                .reviewCount(count)
                .ratingText(ratingText)
                .latestReviewComment(latestComment)
                .build();
    }

    private HotelResponseDto mapToResponseDtoWithRooms(Hotel hotel) {
        List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
        BigDecimal minPrice = (hotel.getPricePerNight() != null && hotel.getPricePerNight().compareTo(BigDecimal.ZERO) > 0)
                ? hotel.getPricePerNight()
                : rooms.stream()
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

        List<Review> hotelReviews = (reviewRepository != null) 
                ? reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotel.getId()) 
                : Collections.emptyList();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<com.re.travelokahotelbookingservice.dto.response.ReviewResponseDto> reviewDtos = hotelReviews.stream()
                .map(rev -> {
                    String author = "Khách lưu trú";
                    String avatar = null;
                    if (rev.getUser() != null) {
                        avatar = rev.getUser().getAvatarUrl();
                        if (rev.getUser().getFullName() != null && !rev.getUser().getFullName().isBlank()) {
                            author = rev.getUser().getFullName();
                        } else if (rev.getUser().getUsername() != null) {
                            author = rev.getUser().getUsername();
                        }
                    }
                    String formattedDate = rev.getCreatedAt() != null ? rev.getCreatedAt().format(dtf) : "Gần đây";
                    return com.re.travelokahotelbookingservice.dto.response.ReviewResponseDto.builder()
                            .id(rev.getId())
                            .hotelId(hotel.getId())
                            .bookingId(rev.getBooking() != null ? rev.getBooking().getId() : null)
                            .userId(rev.getUser() != null ? rev.getUser().getId() : null)
                            .author(author)
                            .userAvatar(avatar)
                            .rating(rev.getRating())
                            .comment(rev.getComment())
                            .date(formattedDate)
                            .createdAt(rev.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        double avgRating = hotelReviews.isEmpty() ? 0.0 : hotelReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        avgRating = Math.round(avgRating * 10.0) / 10.0;

        String ratingText = null;
        if (!hotelReviews.isEmpty() && avgRating > 0) {
            ratingText = avgRating >= 9.0 ? "Xuất sắc" : (avgRating >= 8.0 ? "Tuyệt vời" : (avgRating >= 7.0 ? "Rất tốt" : "Hài lòng"));
        }
        String latestComment = hotelReviews.isEmpty() ? null : hotelReviews.get(0).getComment();

        return HotelResponseDto.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .starRating(hotel.getStarRating())
                .thumbnailUrl(hotel.getThumbnailUrl())
                .images(extractImageList(hotel))
                .amenities(hotel.getAmenities())
                .minPrice(minPrice)
                .totalRooms(rooms.size())
                .createdAt(hotel.getCreatedAt())
                .rooms(roomDtos)
                .reviews(reviewDtos)
                .averageRating(hotelReviews.isEmpty() ? null : avgRating)
                .reviewCount(hotelReviews.size())
                .ratingText(ratingText)
                .latestReviewComment(latestComment)
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
            return new RestPageResponse<>(Collections.emptyList(), pageable, 0);
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

        // Kiểm tra các khách sạn đã từng được cấu hình phòng trong CSDL
        List<Object[]> minPriceRows = roomRepository.findMinPricesByHotelIds(hotelIds);
        Map<Long, BigDecimal> configuredHotelsMap = minPriceRows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1],
                        (existing, replacement) -> existing
                ));

        Map<Long, Double> avgRatingMap = new HashMap<>();
        Map<Long, Integer> reviewCountMap = new HashMap<>();
        Map<Long, String> latestCommentMap = new HashMap<>();
        if (reviewRepository != null && !hotelIds.isEmpty()) {
            List<Object[]> stats = reviewRepository.findReviewStatsByHotelIds(hotelIds);
            for (Object[] row : stats) {
                Long hid = (Long) row[0];
                Double avg = ((Number) row[1]).doubleValue();
                Long count = ((Number) row[2]).longValue();
                avgRatingMap.put(hid, Math.round(avg * 10.0) / 10.0);
                reviewCountMap.put(hid, count.intValue());
            }
            List<Review> latestReviews = reviewRepository.findLatestReviewsByHotelIds(hotelIds);
            for (Review r : latestReviews) {
                if (r.getHotel() != null && r.getComment() != null) {
                    latestCommentMap.put(r.getHotel().getId(), r.getComment());
                }
            }
        }

        // 4. Map sang DTO:
        // - Khách sạn đã có phòng nhưng hết phòng trống trong khoảng ngày chọn -> loại bỏ (return null)
        // - Khách sạn mới tạo chưa thêm phòng (!hasConfiguredRooms) -> vẫn hiển thị để đồng bộ
        List<HotelSearchResponseDto> content = hotels.stream()
                .map(hotel -> {
                    List<Room> availableRooms = roomsByHotelId.getOrDefault(hotel.getId(), Collections.emptyList());
                    boolean hasConfiguredRooms = configuredHotelsMap.containsKey(hotel.getId());

                    // Nếu khách sạn đã có phòng nhưng hiện tại không còn phòng trống phù hợp → loại hotel
                    if (hasConfiguredRooms && availableRooms.isEmpty()) {
                        return null;
                    }

                    // Tính minPricePerNight
                    BigDecimal minPrice = (hotel.getPricePerNight() != null && hotel.getPricePerNight().compareTo(BigDecimal.ZERO) > 0)
                            ? hotel.getPricePerNight()
                            : availableRooms.stream()
                                .map(Room::getPricePerNight)
                                .min(BigDecimal::compareTo)
                                .orElse(configuredHotelsMap.getOrDefault(hotel.getId(), null));

                    Double avgRating = avgRatingMap.get(hotel.getId());
                    Integer reviewCount = reviewCountMap.getOrDefault(hotel.getId(), 0);
                    String latestComment = latestCommentMap.get(hotel.getId());
                    String ratingText = null;
                    if (avgRating != null && reviewCount > 0) {
                        ratingText = avgRating >= 9.0 ? "Xuất sắc" : (avgRating >= 8.0 ? "Tuyệt vời" : (avgRating >= 7.0 ? "Rất tốt" : "Hài lòng"));
                    }

                    return HotelSearchResponseDto.builder()
                            .id(hotel.getId())
                            .name(hotel.getName())
                            .city(hotel.getCity())
                            .address(hotel.getAddress())
                            .starRating(hotel.getStarRating())
                            .thumbnailUrl(hotel.getThumbnailUrl())
                            .images(extractImageList(hotel))
                            .minPricePerNight(minPrice)
                            .averageRating(avgRating)
                            .reviewCount(reviewCount)
                            .latestReviewComment(latestComment)
                            .ratingText(ratingText)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return new RestPageResponse<>(content, pageable, content.size());
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
            return new RestPageResponse<>(Collections.emptyList(), pageable, 0);
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

        Map<Long, Double> avgRatingMap = new HashMap<>();
        Map<Long, Integer> reviewCountMap = new HashMap<>();
        Map<Long, String> latestCommentMap = new HashMap<>();
        if (reviewRepository != null && !hotelIds.isEmpty()) {
            List<Object[]> stats = reviewRepository.findReviewStatsByHotelIds(hotelIds);
            for (Object[] row : stats) {
                Long hid = (Long) row[0];
                Double avg = ((Number) row[1]).doubleValue();
                Long count = ((Number) row[2]).longValue();
                avgRatingMap.put(hid, Math.round(avg * 10.0) / 10.0);
                reviewCountMap.put(hid, count.intValue());
            }
            List<Review> latestReviews = reviewRepository.findLatestReviewsByHotelIds(hotelIds);
            for (Review r : latestReviews) {
                if (r.getHotel() != null && r.getComment() != null) {
                    latestCommentMap.put(r.getHotel().getId(), r.getComment());
                }
            }
        }

        List<HotelListItemDto> content = hotels.stream()
                .map(hotel -> {
                    BigDecimal minPrice = (hotel.getPricePerNight() != null && hotel.getPricePerNight().compareTo(BigDecimal.ZERO) > 0)
                            ? hotel.getPricePerNight()
                            : minPriceMap.get(hotel.getId());
                    Double avgRating = avgRatingMap.get(hotel.getId());
                    Integer reviewCount = reviewCountMap.getOrDefault(hotel.getId(), 0);
                    String latestComment = latestCommentMap.get(hotel.getId());
                    String ratingText = null;
                    if (avgRating != null && reviewCount > 0) {
                        ratingText = avgRating >= 9.0 ? "Xuất sắc" : (avgRating >= 8.0 ? "Tuyệt vời" : (avgRating >= 7.0 ? "Rất tốt" : "Hài lòng"));
                    }

                    return HotelListItemDto.builder()
                            .id(hotel.getId())
                            .name(hotel.getName())
                            .city(hotel.getCity())
                            .address(hotel.getAddress())
                            .starRating(hotel.getStarRating())
                            .thumbnailUrl(hotel.getThumbnailUrl())
                            .images(extractImageList(hotel))
                            .minPricePerNight(minPrice)
                            .averageRating(avgRating)
                            .reviewCount(reviewCount)
                            .latestReviewComment(latestComment)
                            .ratingText(ratingText)
                            .build();
                })
                .toList();

        return new RestPageResponse<>(content, pageable, hotelPage.getTotalElements());
    }

    /**
     * Helper trích xuất danh sách hình ảnh của Khách sạn (xử lý tại tầng Service theo SRP)
     */
    private List<String> extractImageList(Hotel hotel) {
        if (hotel == null) {
            return Collections.emptyList();
        }
        String images = hotel.getImages();
        String thumbnailUrl = hotel.getThumbnailUrl();

        if (images != null && !images.isBlank()) {
            String trimmed = images.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                try {
                    List<String> list = OBJECT_MAPPER.readValue(trimmed, IMAGE_LIST_TYPE_REF);
                    if (list != null && !list.isEmpty()) {
                        return list.stream()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                    }
                } catch (Exception ignored) {}
            }
            if (trimmed.contains(";;;")) {
                return Arrays.stream(trimmed.split(";;;"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            if (trimmed.contains("|||")) {
                return Arrays.stream(trimmed.split("\\|\\|\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            if (trimmed.contains("\n")) {
                return Arrays.stream(trimmed.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            return List.of(thumbnailUrl.trim());
        }
        return Collections.emptyList();
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

