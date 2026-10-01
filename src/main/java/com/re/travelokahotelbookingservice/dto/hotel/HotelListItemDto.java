package com.re.travelokahotelbookingservice.dto.hotel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HotelListItemDto {
    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer starRating;
    private String thumbnailUrl;
    private BigDecimal minPricePerNight;
}
