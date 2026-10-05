package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.CreatePaymentRequest;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.PaymentResponseDto;
import com.re.travelokahotelbookingservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Tạo yêu cầu thanh toán (VNPay Sandbox hoặc Tiền mặt)
     */
    @PostMapping("/create-url")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String username = authentication.getName();
        PaymentResponseDto response = paymentService.createPayment(request, username, httpRequest);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(200, "SUCCESS", response));
    }

    /**
     * Xử lý kết quả trả về từ cổng thanh toán VNPay sau khi khách hàng hoàn tất giao dịch
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> handleVNPayCallback(
            @RequestParam Map<String, String> allParams
    ) {
        PaymentResponseDto response = paymentService.handleVNPayCallback(allParams);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", response));
    }

    /**
     * Tra cứu thông tin thanh toán theo mã đơn đặt phòng
     */
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByBookingId(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        PaymentResponseDto response = paymentService.getPaymentByBookingId(bookingId, username);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", response));
    }
}
