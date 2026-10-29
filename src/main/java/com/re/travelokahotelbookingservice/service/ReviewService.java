package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.CreateReviewRequest;
import com.re.travelokahotelbookingservice.dto.response.ReviewResponseDto;

import java.util.List;

public interface ReviewService {

    ReviewResponseDto createReview(CreateReviewRequest request, String currentUsername);

    List<ReviewResponseDto> getReviewsByHotelId(Long hotelId);

    boolean canReviewBooking(Long bookingId, String currentUsername);
}
