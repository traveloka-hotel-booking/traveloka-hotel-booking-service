package com.re.travelokahotelbookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer maxGuests;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "status", nullable = false)
    private String status; // AVAILABLE, BOOKED, MAINTENANCE

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "bed_type")
    private String bedType;

    @Column(name = "room_size")
    private String roomSize;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "AVAILABLE";
        }
    }
}