package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.LoginRequest;
import com.re.travelokahotelbookingservice.dto.request.RefreshTokenRequest;
import com.re.travelokahotelbookingservice.dto.request.UserRegisterRequest;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.LoginResponse;
import com.re.travelokahotelbookingservice.dto.response.RefreshTokenResponse;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import com.re.travelokahotelbookingservice.serivce.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegisterResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        UserRegisterResponse userRegisterResponse = authService.handleRegister(request);
        ApiResponse<UserRegisterResponse> apiResponse = new ApiResponse<>(200, "SUCCESS", userRegisterResponse);
        return ResponseEntity.ok().body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.handleLogin(request);
        ApiResponse<LoginResponse> apiResponse = new ApiResponse<>(200, "SUCCESS", loginResponse);
        return ResponseEntity.ok().body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> responseToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse refreshTokenResponse = authService.handleRefreshToken(request);
        ApiResponse<RefreshTokenResponse> apiResponse = new ApiResponse<>(200, "SUCCESS", refreshTokenResponse);
        return ResponseEntity.ok().body(apiResponse);
    }
}
