package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.CreateReviewRequest;
import com.re.travelokahotelbookingservice.dto.response.ReviewResponseDto;
import com.re.travelokahotelbookingservice.entity.Booking;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Review;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.entity.enums.BookingStatus;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.repository.BookingRepository;
import com.re.travelokahotelbookingservice.repository.ReviewRepository;
import com.re.travelokahotelbookingservice.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotel_detail", allEntries = true),
            @CacheEvict(value = "hotels_list", allEntries = true),
            @CacheEvict(value = "filter_hotels", allEntries = true),
            @CacheEvict(value = "search_hotels", allEntries = true)
    })
    @Transactional
    public ReviewResponseDto createReview(CreateReviewRequest request, String currentUsername) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng với ID: " + request.getBookingId()));

        User bookingUser = booking.getUser();
        if (bookingUser != null) {
            String email = bookingUser.getEmail();
            String username = bookingUser.getUsername();
            if (!currentUsername.equalsIgnoreCase(email) && !currentUsername.equalsIgnoreCase(username)) {
                throw new BusinessException("Bạn không có quyền đánh giá đơn đặt phòng của người khác!");
            }
        }

        // Quy tắc nghiệp vụ: Chỉ cho phép đánh giá khi khách đã hoàn tất kỳ nghỉ (COMPLETED)
        if (!BookingStatus.COMPLETED.equals(booking.getStatus())) {
            throw new BusinessException("Bạn chỉ có thể gửi đánh giá sau khi đã hoàn tất kỳ nghỉ (Đã trả phòng / Check-out)!");
        }

        // Chống đánh giá trùng lặp: Mỗi kỳ nghỉ chỉ được gửi đánh giá 1 lần duy nhất
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new BusinessException("Đơn đặt phòng này đã được gửi đánh giá từ trước!");
        }

        Hotel hotel = (booking.getRoom() != null) ? booking.getRoom().getHotel() : null;
        if (hotel == null) {
            throw new BusinessException("Không tìm thấy thông tin khách sạn tương ứng với đơn đặt phòng!");
        }

        Review review = Review.builder()
                .hotel(hotel)
                .user(bookingUser)
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment().trim())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Khách hàng {} đã đánh giá khách sạn {} (ID: {}) với số điểm {}/10",
                currentUsername, hotel.getName(), hotel.getId(), request.getRating());

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByHotelId(Long hotelId) {
        if (hotelId == null) return Collections.emptyList();
        return reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReviewBooking(Long bookingId, String currentUsername) {
        if (bookingId == null) return false;
        return bookingRepository.findById(bookingId).map(b -> {
            boolean isOwner = false;
            if (b.getUser() != null) {
                isOwner = currentUsername.equalsIgnoreCase(b.getUser().getEmail())
                        || currentUsername.equalsIgnoreCase(b.getUser().getUsername());
            }
            boolean isCompleted = BookingStatus.COMPLETED.equals(b.getStatus());
            boolean notReviewed = !reviewRepository.existsByBookingId(b.getId());
            return isOwner && isCompleted && notReviewed;
        }).orElse(false);
    }

    private ReviewResponseDto mapToDto(Review review) {
        if (review == null) return null;

        String author = "Khách lưu trú";
        String avatar = null;
        Long userId = null;

        if (review.getUser() != null) {
            userId = review.getUser().getId();
            avatar = review.getUser().getAvatarUrl();
            if (review.getUser().getFullName() != null && !review.getUser().getFullName().isBlank()) {
                author = review.getUser().getFullName();
            } else if (review.getUser().getUsername() != null) {
                author = review.getUser().getUsername();
            }
        }

        String formattedDate = review.getCreatedAt() != null 
                ? review.getCreatedAt().format(DATE_FORMATTER) 
                : "Gần đây";

        return ReviewResponseDto.builder()
                .id(review.getId())
                .hotelId(review.getHotel() != null ? review.getHotel().getId() : null)
                .bookingId(review.getBooking() != null ? review.getBooking().getId() : null)
                .userId(userId)
                .author(author)
                .userAvatar(avatar)
                .rating(review.getRating())
                .comment(review.getComment())
                .date(formattedDate)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
