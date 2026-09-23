package com.re.travelokahotelbookingservice.serivce;

import com.re.travelokahotelbookingservice.dto.request.LoginRequest;
import com.re.travelokahotelbookingservice.dto.request.RefreshTokenRequest;
import com.re.travelokahotelbookingservice.dto.request.UserRegisterRequest;
import com.re.travelokahotelbookingservice.dto.response.LoginResponse;
import com.re.travelokahotelbookingservice.dto.response.RefreshTokenResponse;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;

public interface AuthService {
    UserRegisterResponse handleRegister(UserRegisterRequest request);
    LoginResponse handleLogin(LoginRequest request);
    RefreshTokenResponse handleRefreshToken(RefreshTokenRequest request);
}
