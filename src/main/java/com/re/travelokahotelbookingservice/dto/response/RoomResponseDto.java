package com.re.travelokahotelbookingservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponseDto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String name;
    private String description;
    private Integer maxGuests;
    private BigDecimal pricePerNight;
    private String status;
    private String imageUrl;
    private String bedType;
    private String roomSize;
    private String amenities;
    private LocalDateTime createdAt;
}
