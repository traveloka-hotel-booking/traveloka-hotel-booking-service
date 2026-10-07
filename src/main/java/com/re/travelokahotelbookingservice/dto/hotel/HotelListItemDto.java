package com.re.travelokahotelbookingservice.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelListItemDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer starRating;
    private String thumbnailUrl;
    private BigDecimal minPricePerNight;
}

