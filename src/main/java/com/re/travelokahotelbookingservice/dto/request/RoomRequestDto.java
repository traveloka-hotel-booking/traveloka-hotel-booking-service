package com.re.travelokahotelbookingservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequestDto {
    @NotNull(message = "ID khách sạn không được để trống")
    private Long hotelId;

    @NotBlank(message = "Tên loại phòng không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Số lượng khách tối đa không được để trống")
    @Min(value = 1, message = "Số lượng khách tối thiểu là 1 người")
    private Integer maxGuests;

    @NotNull(message = "Giá phòng mỗi đêm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phòng mỗi đêm phải lớn hơn 0")
    private BigDecimal pricePerNight;

    private String status; // AVAILABLE, BOOKED, MAINTENANCE
    private String imageUrl;
    private String bedType;
    private String roomSize;
    private String amenities;
}
