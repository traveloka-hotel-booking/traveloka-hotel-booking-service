package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.service.HotelService;
import com.re.travelokahotelbookingservice.service.RoomService;
import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelListItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HotelResponseDto>>> getHotels(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<HotelResponseDto> result = hotelService.getAllHotels(search, city, pageable);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponseDto>> getHotelById(@PathVariable Long id) {
        HotelResponseDto result = hotelService.getHotelById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponseDto>>> getHotelRooms(@PathVariable Long id) {
        List<RoomResponseDto> result = roomService.getRoomsByHotelId(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    // FR-06: Tìm kiếm khách sạn
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<HotelSearchResponseDto>>> searchHotels(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam Integer guests,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<HotelSearchResponseDto> result = hotelService.searchHotels(city, checkIn, checkOut, guests, pageable);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    // FR-07: Lọc khách sạn
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<HotelListItemDto>>> filterHotels(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer minStar,
            @RequestParam(required = false) Integer maxStar,
            @RequestParam(required = false) String amenities,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        HotelFilterRequest filter = new HotelFilterRequest();
        filter.setCity(city);
        filter.setMinStar(minStar);
        filter.setMaxStar(maxStar);

        // Parse chuỗi amenities → List<String>
        if (amenities != null && !amenities.isBlank()) {
            filter.setAmenities(
                    Arrays.stream(amenities.split(","))
                            .map(String::trim)
                            .toList()
            );
        }

        Page<HotelListItemDto> result = hotelService.filterHotels(filter, pageable);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }


}
