package com.re.travelokahotelbookingservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelResponseDto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private Integer starRating;
    private String thumbnailUrl;
    private List<String> images;
    private String amenities;
    private BigDecimal minPrice;
    private Integer totalRooms;
    private LocalDateTime createdAt;
    private List<RoomResponseDto> rooms;
    private List<ReviewResponseDto> reviews;
    private Double averageRating;
    private Integer reviewCount;
}
