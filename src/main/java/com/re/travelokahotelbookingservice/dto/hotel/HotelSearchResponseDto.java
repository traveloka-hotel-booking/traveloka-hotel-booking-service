package com.re.travelokahotelbookingservice.dto.hotel;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchResponseDto {
    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer starRating;
    private String thumbnailUrl;
    private BigDecimal minPricePerNight;
}