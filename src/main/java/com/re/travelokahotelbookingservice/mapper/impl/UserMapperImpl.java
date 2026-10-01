package com.re.travelokahotelbookingservice.mapper.impl;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserRegisterRequest;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import com.re.travelokahotelbookingservice.entity.Role;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.mapper.UserMapper;
import com.re.travelokahotelbookingservice.security.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapperImpl implements UserMapper {
    @Override
    public User toUserRegisterRequest(UserRegisterRequest request, String passwordEncoder) {
        return User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .password(passwordEncoder)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.ROLE_CUSTOMER)
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public UserRegisterResponse toUser(User user) {
        return UserRegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isEnabled(user.getIsEnabled())
                .build();
    }

    @Override
    public UserDto toUserLogin(Object principal) {


        if(principal instanceof UserDetailsImpl userDetails) {
            Role role = Role.valueOf(userDetails.getAuthorities()
                    .stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse(null));

            return UserDto.builder()
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .email(userDetails.getEmail())
                    .fullName(userDetails.getFullName())
                    .yob(userDetails.getYob())
                    .gender(userDetails.getGender())
                    .address(userDetails.getAddress())
                    .phone(userDetails.getPhone())
                    .role(role)
                    .isEnabled(userDetails.isEnabled())
                    .createdAt(userDetails.getCreatedAt())
                    .build();
        }
        return null;
    }

    @Override
    public UserDto fromUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .yob(user.getYob())
                .gender(user.getGender())
                .address(user.getAddress())
                .phone(user.getPhone())
                .role(user.getRole())
                .isEnabled(user.getIsEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
