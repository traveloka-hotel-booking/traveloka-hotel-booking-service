package com.re.travelokahotelbookingservice.dto.response;

import com.re.travelokahotelbookingservice.entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private Boolean isEnabled;
}
