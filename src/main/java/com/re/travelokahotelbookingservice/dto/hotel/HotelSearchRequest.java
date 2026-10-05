package com.re.travelokahotelbookingservice.dto.hotel;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HotelSearchRequest {
    private String city;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
}
