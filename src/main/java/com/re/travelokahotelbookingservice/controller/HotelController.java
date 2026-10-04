package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.service.HotelService;
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

}
