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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    private ResponseCookie createSecureCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value != null ? value : "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSameSite)
                .build();
    }

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

        ResponseCookie accessCookie = createSecureCookie("accessToken", loginResponse.getAccessToken(), 15 * 60);
        ResponseCookie refreshCookie = createSecureCookie("refreshToken", loginResponse.getRefreshToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> responseToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse refreshTokenResponse = authService.handleRefreshToken(request);
        ApiResponse<RefreshTokenResponse> apiResponse = new ApiResponse<>(200, "SUCCESS", refreshTokenResponse);

        ResponseCookie accessCookie = createSecureCookie("accessToken", refreshTokenResponse.getAccessToken(), 15 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request, @RequestBody @Valid LogoutRequest logoutRequest) {
        authService.handleLogout(request, logoutRequest.getRefreshToken());
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", null);

        ResponseCookie clearAccessCookie = createSecureCookie("accessToken", "", 0);
        ResponseCookie clearRefreshCookie = createSecureCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(apiResponse);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(Authentication authentication, HttpServletRequest requestHeader, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication, requestHeader, request);
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", "Cập nhật mật khẩu thành công");
        return ResponseEntity.ok().body(apiResponse);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", "Gửi mail reset password thành công");
        return ResponseEntity.ok().body(apiResponse);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", "Cập nhật mật khẩu mới thành công");
        return ResponseEntity.ok().body(apiResponse);
    }
}
