package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.ProfileUpdateRequestDto;
import com.re.travelokahotelbookingservice.dto.response.UserProfileResponseDto;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getCurrentUserProfile(String username) {
        User user = findUserByUsernameOrEmail(username);
        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateProfile(String username, ProfileUpdateRequestDto request, MultipartFile avatar) {
        User user = findUserByUsernameOrEmail(username);

        // Upload avatar lên Cloudinary nếu có gửi file
        if (avatar != null && !avatar.isEmpty()) {
            String uploadedUrl = cloudinaryService.uploadImage(avatar, "traveloka/avatars");
            user.setAvatarUrl(uploadedUrl);
            log.info("Updated avatar for user {}: {}", username, uploadedUrl);
        }

        if (request != null) {
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                user.setFullName(request.getFullName().trim());
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                user.setPhone(request.getPhone().trim());
            }
            if (request.getGender() != null && !request.getGender().isBlank()) {
                user.setGender(request.getGender().trim());
            }
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
                user.setDateOfBirth(request.getDateOfBirth().trim());
            }
        }

        User saved = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", username);
        return mapToDto(saved);
    }

    private User findUserByUsernameOrEmail(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với định danh: " + username));
    }

    private UserProfileResponseDto mapToDto(User user) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
