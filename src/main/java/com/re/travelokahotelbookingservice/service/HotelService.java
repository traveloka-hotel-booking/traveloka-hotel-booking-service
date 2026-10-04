package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.HotelRequestDto;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.dto.hotel.HotelSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDate;

public interface HotelService {
    Page<HotelResponseDto> getAllHotels(String search, String city, Pageable pageable);
    List<HotelResponseDto> getAllHotelsList();
    HotelResponseDto getHotelById(Long id);
    HotelResponseDto createHotel(HotelRequestDto request);
    HotelResponseDto updateHotel(Long id, HotelRequestDto request);
    void deleteHotel(Long id);
    HotelResponseDto updateThumbnail(Long id, MultipartFile file);
    String uploadHotelImage(MultipartFile file);

    // FR-06
    Page<HotelSearchResponseDto> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, Integer guests);

}

