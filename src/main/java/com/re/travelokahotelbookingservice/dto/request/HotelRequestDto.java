package com.re.travelokahotelbookingservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelRequestDto {
    @NotBlank(message = "Tên khách sạn không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Thành phố không được để trống")
    private String city;

    @Min(value = 1, message = "Hạng sao tối thiểu là 1")
    @Max(value = 5, message = "Hạng sao tối đa là 5")
    private Integer starRating;

    private String thumbnailUrl;
    private String amenities;
}
