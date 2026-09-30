package com.re.travelokahotelbookingservice.dto;

import com.re.travelokahotelbookingservice.entity.enums.Role;
import lombok.*;

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
    private String phone;
    private Role role;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
}
