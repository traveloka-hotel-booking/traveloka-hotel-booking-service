package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import com.re.travelokahotelbookingservice.dto.hotel.HotelListItemDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface HotelService {

    // FR-06
    Page<HotelSearchResponseDto> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, Integer guests);

    // FR-07
    Page<HotelListItemDto> filterHotels(HotelFilterRequest filter, Pageable pageable);
}

