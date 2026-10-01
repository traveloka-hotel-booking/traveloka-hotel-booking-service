package com.re.travelokahotelbookingservice.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequestDto {
    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer numberOfGuests;
}
