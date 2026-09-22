package com.re.travelokahotelbookingservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    private String city;

    private Integer starRating;

    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private LocalDateTime createdAt;

    // getters, setters
}
