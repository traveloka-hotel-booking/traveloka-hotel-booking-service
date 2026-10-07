package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.*;
import com.re.travelokahotelbookingservice.dto.response.LoginResponse;
import com.re.travelokahotelbookingservice.dto.response.RefreshTokenResponse;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface AuthService {
    UserRegisterResponse handleRegister(UserRegisterRequest request);
    LoginResponse handleLogin(LoginRequest request);
    RefreshTokenResponse handleRefreshToken(RefreshTokenRequest request);
    void handleLogout(HttpServletRequest request, String refreshToken);
    void changePassword(Authentication authentication, HttpServletRequest requestHeader, ChangePasswordRequest request);
    String forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
