package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.*;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.LoginResponse;
import com.re.travelokahotelbookingservice.dto.response.RefreshTokenResponse;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import com.re.travelokahotelbookingservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request, @RequestBody @Valid LogoutRequest logoutRequest) {
        authService.handleLogout(request, logoutRequest.getRefreshToken());
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", null);
        return ResponseEntity.ok().body(apiResponse);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(Authentication authentication, HttpServletRequest requestHeader, @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication, requestHeader, request);
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", "Cập nhật mật khẩu thành công");
        return ResponseEntity.ok().body(apiResponse);
    }
}
