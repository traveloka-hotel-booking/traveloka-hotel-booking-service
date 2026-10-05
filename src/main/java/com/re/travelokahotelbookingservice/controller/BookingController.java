package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.BookingRequestDto;
import com.re.travelokahotelbookingservice.dto.BookingResponseDto;
import com.re.travelokahotelbookingservice.dto.response.AdminBookingDto;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponseDto> createBooking(
            @RequestBody BookingRequestDto request,
            Authentication authentication
    ) {
       String username = authentication.getName();

        BookingResponseDto response = bookingService.createBooking(request, username);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Khách hàng xem lịch sử các đơn đặt phòng của chính mình
     */
    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminBookingDto>>> getMyBookings(Authentication authentication) {
        String username = authentication.getName();
        List<AdminBookingDto> bookings = bookingService.getMyBookings(username);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", bookings));
    }

    /**
     * Khách hàng tự hủy đơn đặt phòng của mình (nếu còn trạng thái PENDING hoặc CONFIRMED)
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AdminBookingDto>> cancelMyBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        AdminBookingDto booking = bookingService.cancelBookingCustomer(id, username);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", booking));
    }
}

