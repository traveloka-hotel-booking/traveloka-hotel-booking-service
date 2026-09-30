package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.BookingRequestDto;
import com.re.travelokahotelbookingservice.dto.BookingResponseDto;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request, String currentUsername);
}