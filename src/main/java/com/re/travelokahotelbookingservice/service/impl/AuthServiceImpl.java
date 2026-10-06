package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.*;
import com.re.travelokahotelbookingservice.dto.response.LoginResponse;
import com.re.travelokahotelbookingservice.dto.response.RefreshTokenResponse;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import com.re.travelokahotelbookingservice.entity.RefreshToken;
import com.re.travelokahotelbookingservice.entity.ResetPasswordToken;
import com.re.travelokahotelbookingservice.entity.TokenBlackList;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.exception.EmailAlreadyExistsException;
import com.re.travelokahotelbookingservice.mapper.UserMapper;
import com.re.travelokahotelbookingservice.repository.RefreshTokenRepository;
import com.re.travelokahotelbookingservice.repository.ResetPasswordTokenRepository;
import com.re.travelokahotelbookingservice.repository.TokenBlackListRepository;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import com.re.travelokahotelbookingservice.security.TokenProvider;
import com.re.travelokahotelbookingservice.service.AuthService;
import com.re.travelokahotelbookingservice.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlackListRepository tokenBlackListRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;
    private final EmailService emailService;

    @Value("${jwt.accessTokenExpiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refreshTokenExpiration}")
    private Long refreshTokenExpiration;

    @Override
    public UserRegisterResponse handleRegister(UserRegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không trùng khớp");
        }
        User user = userMapper.toUserRegisterRequest(request, passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return userMapper.toUser(user);
    }

    @Transactional
    @Override
    public LoginResponse handleLogin(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);

        Object principal = authentication.getPrincipal();

        UserDto userDto = userMapper.toUserLogin(principal);

        User user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByUsername(request.getEmail()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .refreshToken(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .message("Login successfully")
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .user(userDto)
                .build();
    }

    @Transactional
    @Override
    public RefreshTokenResponse handleRefreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));

        if(refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }

        User user = refreshToken.getUser();

        String accessToken = tokenProvider.generateAccessTokenFromEmail(user.getEmail());
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .build();

    }

    @Transactional
    @Override
    public void handleLogout(HttpServletRequest request, String refreshToken) {

        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("Access token is missing");
        }

        String accessToken = authorization.substring(7);
        Instant expDate = tokenProvider.getExpiryDate(accessToken);

        TokenBlackList tokenBlackList = TokenBlackList.builder()
                .accessToken(accessToken)
                .expDate(expDate)
                .build();
        tokenBlackListRepository.save(tokenBlackList);
        refreshTokenRepository.findByRefreshToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    @Override
    public void changePassword(Authentication authentication, HttpServletRequest requestHeader, ChangePasswordRequest request) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu không đúng");
        }
        if(passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        String authorization = requestHeader.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("Invalid access token");
        }

        String accessToken = authorization.substring(7);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        Instant expDate = tokenProvider.getExpiryDate(accessToken);

        TokenBlackList tokenBlackList = TokenBlackList.builder()
                .accessToken(accessToken)
                .expDate(expDate)
                .build();

        tokenBlackListRepository.save(tokenBlackList);

        refreshTokenRepository.deleteByUserId(user.getId());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        ResetPasswordToken resetPasswordToken = ResetPasswordToken.builder()
                .resetToken(resetToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(900))
                .used(false)
                .build();
        resetPasswordTokenRepository.save(resetPasswordToken);
        emailService.sendEmail(request.getEmail(), resetToken);
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
       ResetPasswordToken resetPasswordToken = resetPasswordTokenRepository.findByResetToken(request.getToken())
               .orElseThrow(() -> new EntityNotFoundException("Reset token không hợp lệ"));

       if(resetPasswordToken.getExpiryDate().isBefore(LocalDateTime.now())) {
           throw new IllegalStateException("Reset token đã hết hạn");
       }
       User user = resetPasswordToken.getUser();
       String newPassword = passwordEncoder.encode(request.getNewPassword());
       user.setPassword(newPassword);
       userRepository.save(user);
       resetPasswordToken.setUsed(true);
       resetPasswordTokenRepository.save(resetPasswordToken);
    }


}
