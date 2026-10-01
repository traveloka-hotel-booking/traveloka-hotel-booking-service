package com.re.travelokahotelbookingservice.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequestDto {
    private String fullName;
    private String phone;
    private String gender;
    private String dateOfBirth;
}
