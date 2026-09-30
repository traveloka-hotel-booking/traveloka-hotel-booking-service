package com.re.travelokahotelbookingservice.dto.response;

import com.re.travelokahotelbookingservice.entity.enums.PaymentMethod;
import com.re.travelokahotelbookingservice.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long paymentId;
    private Long bookingId;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentUrl;
    private LocalDateTime paymentDate;
    private String message;
}
