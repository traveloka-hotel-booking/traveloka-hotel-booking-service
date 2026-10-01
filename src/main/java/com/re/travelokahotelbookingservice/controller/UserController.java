package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserUpdateRequest;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/customer")
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(Authentication authentication) {
        UserDto userDto = userService.getUser(authentication);
        ApiResponse<UserDto> apiResponse = new ApiResponse<>(200, "SUCCESS", userDto);
        return ResponseEntity.ok().body(apiResponse);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(Authentication authentication, @Valid @RequestBody UserUpdateRequest updateRequest) {
        UserDto userDto = userService.updateUser(authentication, updateRequest);
        ApiResponse<UserDto> apiResponse = new ApiResponse<>(200, "SUCCESS", userDto);
        return ResponseEntity.ok().body(apiResponse);
    }
}
