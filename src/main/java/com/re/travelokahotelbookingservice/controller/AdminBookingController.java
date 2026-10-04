package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.response.AdminBookingDto;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    /**
     * Lấy toàn bộ danh sách đơn đặt phòng trong hệ thống cho Admin
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBookingDto>>> getAllBookings() {
        List<AdminBookingDto> bookings = bookingService.getAllBookingsAdmin();
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", bookings));
    }

    /**
     * Lấy chi tiết đơn đặt phòng theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBookingDto>> getBookingById(@PathVariable Long id) {
        AdminBookingDto booking = bookingService.getBookingByIdAdmin(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", booking));
    }

    /**
     * Duyệt đơn đặt phòng (Chuyển sang CONFIRMED) - Tuân thủ Sơ đồ tuần tự 3.3
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<AdminBookingDto>> confirmBooking(@PathVariable Long id) {
        AdminBookingDto booking = bookingService.confirmBooking(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", booking));
    }

    /**
     * Hủy đơn đặt phòng bởi Admin
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AdminBookingDto>> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Admin hủy đơn") String reason
    ) {
        AdminBookingDto booking = bookingService.cancelBookingAdmin(id, reason);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", booking));
    }

    /**
     * Cập nhật trạng thái đơn đặt phòng linh hoạt (CHECKED_IN, COMPLETED, v.v.)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminBookingDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        AdminBookingDto booking = bookingService.updateBookingStatusAdmin(id, status);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", booking));
    }
}
