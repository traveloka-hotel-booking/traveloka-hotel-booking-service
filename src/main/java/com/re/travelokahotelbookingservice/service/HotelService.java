package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface HotelService {

    // FR-06
    Page<HotelSearchResponseDto> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, Integer guests);


}

