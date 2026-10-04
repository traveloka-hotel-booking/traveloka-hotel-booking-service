package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.service.HotelService;
import com.re.travelokahotelbookingservice.service.RoomService;
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
    public ResponseEntity<?> searchHotels(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam Integer guests
    ) {
        return ResponseEntity.ok(
                hotelService.searchHotels(city, checkIn, checkOut, guests)
        );
    }

}
