package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.BookingRequestDto;
import com.re.travelokahotelbookingservice.dto.response.BookingResponseDto;
import com.re.travelokahotelbookingservice.dto.response.AdminBookingDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request, String currentUsername);

    List<AdminBookingDto> getAllBookingsAdmin();

    AdminBookingDto getBookingByIdAdmin(Long id);

    AdminBookingDto confirmBooking(Long bookingId);

    AdminBookingDto cancelBookingAdmin(Long bookingId, String reason);

    AdminBookingDto updateBookingStatusAdmin(Long bookingId, String status);

    List<AdminBookingDto> getMyBookings(String currentUsername);

    AdminBookingDto cancelBookingCustomer(Long bookingId, String currentUsername);

    AdminBookingDto payCashAdmin(Long bookingId);

    AdminBookingDto checkinAdmin(Long bookingId, java.time.LocalDate actualCheckInDate);

    AdminBookingDto checkoutAdmin(Long bookingId, java.time.LocalDate actualCheckOutDate);

    void deleteBookingAdmin(Long id);

    int cleanupExpiredBookingsAdmin();
}