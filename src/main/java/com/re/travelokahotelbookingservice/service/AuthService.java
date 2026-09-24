package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.LoginRequest;
import com.re.travelokahotelbookingservice.dto.request.RefreshTokenRequest;
import com.re.travelokahotelbookingservice.dto.request.UserRegisterRequest;
import com.re.travelokahotelbookingservice.dto.response.LoginResponse;
import com.re.travelokahotelbookingservice.dto.response.RefreshTokenResponse;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

public interface AuthService {
    UserRegisterResponse handleRegister(UserRegisterRequest request);
    LoginResponse handleLogin(LoginRequest request);
    RefreshTokenResponse handleRefreshToken(RefreshTokenRequest request);
    void handleLogout(HttpServletRequest request, String refreshToken);
}
