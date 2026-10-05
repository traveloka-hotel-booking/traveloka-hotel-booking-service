package com.re.travelokahotelbookingservice.dto.hotel;

import lombok.Data;
import java.util.List;

@Data
public class HotelFilterRequest {
    private String city;
    private Integer minStar;
    private Integer maxStar;
    private List<String> amenities;
}

