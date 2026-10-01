package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.ProfileUpdateRequestDto;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.UserProfileResponseDto;
import com.re.travelokahotelbookingservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileResponseDto response = userService.getCurrentUserProfile(username);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", response));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateProfile(
            Authentication authentication,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        String username = authentication.getName();
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .fullName(fullName)
                .phone(phone)
                .gender(gender)
                .dateOfBirth(dateOfBirth)
                .build();

        UserProfileResponseDto response = userService.updateProfile(username, request, avatar);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", response));
    }
}
