package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.CreateReviewRequest;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.ReviewResponseDto;
import com.re.travelokahotelbookingservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Khách hàng gửi đánh giá cho đơn đặt phòng đã hoàn tất lưu trú (COMPLETED)
     */
    @PostMapping("/api/v1/customer/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        ReviewResponseDto response = reviewService.createReview(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "SUCCESS", response));
    }

    /**
     * Lấy danh sách đánh giá công khai của một khách sạn
     */
    @GetMapping("/api/v1/hotels/{hotelId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getHotelReviews(@PathVariable Long hotelId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsByHotelId(hotelId);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", reviews));
    }

    /**
     * Kiểm tra nhanh xem đơn đặt phòng hiện tại có đủ điều kiện để đánh giá không
     */
    @GetMapping("/api/v1/customer/reviews/can-review/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> canReviewBooking(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        boolean canReview = reviewService.canReviewBooking(bookingId, username);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", canReview));
    }
}
