package com.re.travelokahotelbookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String images;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public java.util.List<String> getImageList() {
        if (images != null && !images.isBlank()) {
            String trimmed = images.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.List<String> list = mapper.readValue(trimmed, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                    if (list != null && !list.isEmpty()) {
                        return list.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
                    }
                } catch (Exception ignored) {}
            }
            if (trimmed.contains(";;;")) {
                return java.util.Arrays.stream(trimmed.split(";;;"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
            if (trimmed.contains("|||")) {
                return java.util.Arrays.stream(trimmed.split("\\|\\|\\|"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
            if (trimmed.contains("\n")) {
                return java.util.Arrays.stream(trimmed.split("\n"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
            return java.util.Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            return java.util.List.of(thumbnailUrl.trim());
        }
        return java.util.Collections.emptyList();
    }
}
