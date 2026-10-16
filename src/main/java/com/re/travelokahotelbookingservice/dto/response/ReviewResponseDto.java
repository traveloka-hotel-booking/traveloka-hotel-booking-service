package com.re.travelokahotelbookingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long hotelId;
    private Long bookingId;
    private Long userId;
    private String author;
    private String userAvatar;
    private Integer rating;
    private String comment;
    private String date;
    private LocalDateTime createdAt;
}
