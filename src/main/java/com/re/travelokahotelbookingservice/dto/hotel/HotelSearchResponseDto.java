package com.re.travelokahotelbookingservice.dto.hotel;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchResponseDto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer starRating;
    private String thumbnailUrl;
    private java.util.List<String> images;
    private BigDecimal minPricePerNight;
    private Double averageRating;
    private Integer reviewCount;
    private String latestReviewComment;
    private String ratingText;
}