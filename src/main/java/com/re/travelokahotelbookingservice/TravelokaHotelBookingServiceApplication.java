package com.re.travelokahotelbookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelokaHotelBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelokaHotelBookingServiceApplication.class, args);
    }

}
