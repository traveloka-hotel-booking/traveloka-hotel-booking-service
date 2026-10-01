package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.service.HotelService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
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

    //FR-07

    @GetMapping
    public ResponseEntity<?> filterHotels(
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

        return ResponseEntity.ok(
                hotelService.filterHotels(filter, pageable)
        );
    }
}
