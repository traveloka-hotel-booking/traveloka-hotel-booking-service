package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserUpdateRequest;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.mapper.UserMapper;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import com.re.travelokahotelbookingservice.security.TokenProvider;
import com.re.travelokahotelbookingservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;

    public UserDto getUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("email is not exist!"));
        return userMapper.fromUser(user);
    }

    @Override
    public UserDto updateUser(Authentication authentication, UserUpdateRequest updateRequest) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(updateRequest.getFullName());
        user.setPhone(updateRequest.getPhone());
        user.setYob(updateRequest.getYob());
        user.setGender(updateRequest.getGender());
        user.setAddress(updateRequest.getAddress());
        userRepository.save(user);
        return userMapper.fromUser(user);
    }
}
