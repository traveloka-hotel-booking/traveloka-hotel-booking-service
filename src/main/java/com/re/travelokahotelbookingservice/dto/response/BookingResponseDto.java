package com.re.travelokahotelbookingservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BookingResponseDto {
    private Long bookingId;
    private BigDecimal totalPrice;
    private String status;
}
