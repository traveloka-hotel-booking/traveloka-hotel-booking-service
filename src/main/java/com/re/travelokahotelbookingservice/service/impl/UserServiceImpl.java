package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserUpdateRequest;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.mapper.UserMapper;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import com.re.travelokahotelbookingservice.security.TokenProvider;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.GoogleGeocodingService;
import com.re.travelokahotelbookingservice.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final CloudinaryService cloudinaryService;
    private final GoogleGeocodingService googleGeocodingService;

    public UserDto getUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.fromUser(user);
    }

    @Transactional
    @Override
    public UserDto updateUser(Authentication authentication, UserUpdateRequest updateRequest) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (updateRequest.getFullName() != null) {
            user.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }

        if (updateRequest.getYob() != null) {
            user.setYob(updateRequest.getYob());
        }

        if (updateRequest.getGender() != null) {
            user.setGender(updateRequest.getGender());
        }

        if (updateRequest.getAddress() != null && !updateRequest.getAddress().isBlank()) {
            String formattedAddress = googleGeocodingService.validateAddress(updateRequest.getAddress());
            user.setAddress(formattedAddress);
        }

        return userMapper.fromUser(user);
    }

    @Transactional
    @Override
    public String updateAvatar(MultipartFile file, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Map uploadImage = cloudinaryService.uploadImage(file, user.getId());
        String avatarUrl = uploadImage.get("secure_url").toString();
        user.setAvatarUrl(avatarUrl);
        return avatarUrl;
    }
}
