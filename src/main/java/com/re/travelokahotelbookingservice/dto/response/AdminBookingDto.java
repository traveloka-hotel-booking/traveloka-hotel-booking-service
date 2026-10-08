package com.re.travelokahotelbookingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bookingCode; // Dạng BK-0001

    // Thông tin khách hàng
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Thông tin khách sạn
    private Long hotelId;
    private String hotelName;
    private String hotelCity;
    private String hotelAddress;
    private String hotelThumbnailUrl;

    // Thông tin phòng
    private Long roomId;
    private String roomName;
    private String roomType;
    private String roomImageUrl;
    private BigDecimal pricePerNight;

    // Thông tin đặt phòng
    private LocalDate checkIn;
    private LocalDate checkOut;
    private long nights;
    private Integer numberOfGuests;
    private BigDecimal totalPrice;
    private String status; // PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED
    private LocalDateTime createdAt;

    // Thông tin thanh toán
    private Long paymentId;
    private String paymentMethod; // CASH, VNPAY
    private String paymentStatus; // PENDING, SUCCESS, FAILED
    private String transactionId;
    private LocalDateTime paymentDate;
}
