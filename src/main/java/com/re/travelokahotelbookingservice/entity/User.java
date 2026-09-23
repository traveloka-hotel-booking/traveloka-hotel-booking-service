package com.re.travelokahotelbookingservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;
    private String phone;
    private String role; // ROLE_ADMIN, ROLE_CUSTOMER
    private boolean isEnabled;

    private LocalDateTime createdAt;

    // getters, setters
}
