package com.re.travelokahotelbookingservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}
