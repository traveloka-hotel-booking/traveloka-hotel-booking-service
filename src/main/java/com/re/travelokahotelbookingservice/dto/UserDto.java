package com.re.travelokahotelbookingservice.dto;

import com.re.travelokahotelbookingservice.entity.enums.Role;
import com.re.travelokahotelbookingservice.entity.enums.Gender;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private LocalDate yob;
    private Gender gender;
    private String address;
    private String phone;
    private Role role;
    private String avatarUrl;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
}
