package com.re.travelokahotelbookingservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponseDto {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String name;
    private Integer maxGuests;
    private BigDecimal pricePerNight;
    private String status;
}
