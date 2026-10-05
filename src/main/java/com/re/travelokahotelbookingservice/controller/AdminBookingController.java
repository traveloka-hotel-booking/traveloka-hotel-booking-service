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

    /**
     * Xác nhận thu tiền mặt cho đơn đặt phòng (Bắt buộc trước khi cho nhận phòng)
     */
    @PutMapping("/{id}/pay-cash")
    public ResponseEntity<ApiResponse<AdminBookingDto>> payCash(@PathVariable Long id) {
        AdminBookingDto booking = bookingService.payCashAdmin(id);
        return ResponseEntity.ok(ApiResponse.<AdminBookingDto>builder()
                .code(200)
                .message("Đã xác nhận thu tiền mặt thành công")
                .result(booking)
                .build());
    }

    /**
     * Nhận phòng linh hoạt: Cho phép chọn ngày nhận phòng thực tế (sớm hoặc trễ) và tính lại tổng tiền bill
     */
    @PutMapping("/{id}/checkin")
    public ResponseEntity<ApiResponse<AdminBookingDto>> checkin(
            @PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate actualCheckInDate
    ) {
        AdminBookingDto booking = bookingService.checkinAdmin(id, actualCheckInDate);
        return ResponseEntity.ok(ApiResponse.<AdminBookingDto>builder()
                .code(200)
                .message("Hoàn tất thủ tục nhận phòng và cập nhật lịch lưu trú thành công")
                .result(booking)
                .build());
    }

    /**
     * Trả phòng linh hoạt: Cho phép chọn ngày trả phòng thực tế và tính lại tổng tiền bill
     */
    @PutMapping("/{id}/checkout")
    public ResponseEntity<ApiResponse<AdminBookingDto>> checkout(
            @PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate actualCheckOutDate
    ) {
        AdminBookingDto booking = bookingService.checkoutAdmin(id, actualCheckOutDate);
        return ResponseEntity.ok(ApiResponse.<AdminBookingDto>builder()
                .code(200)
                .message("Hoàn tất thủ tục trả phòng và quyết toán hóa đơn thành công")
                .result(booking)
                .build());
    }

    /**
     * Dọn dẹp tất cả các đơn đặt phòng quá hạn
     */
    @DeleteMapping("/cleanup-expired")
    public ResponseEntity<ApiResponse<Integer>> cleanupExpiredBookings() {
        int count = bookingService.cleanupExpiredBookingsAdmin();
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .code(200)
                .message("Đã dọn dẹp thành công " + count + " đơn đặt phòng quá hạn")
                .result(count)
                .build());
    }

    /**
     * Xóa đơn đặt phòng theo ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBookingAdmin(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .code(200)
                .message("Đã xóa đơn đặt phòng thành công")
                .result("DELETED")
                .build());
    }
}
