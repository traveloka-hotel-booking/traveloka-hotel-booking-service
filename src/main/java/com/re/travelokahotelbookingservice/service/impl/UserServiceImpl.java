package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserUpdateRequest;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.entity.enums.Role;
import com.re.travelokahotelbookingservice.mapper.UserMapper;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import com.re.travelokahotelbookingservice.security.TokenProvider;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import com.re.travelokahotelbookingservice.service.GoogleGeocodingService;
import com.re.travelokahotelbookingservice.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final com.re.travelokahotelbookingservice.repository.RefreshTokenRepository refreshTokenRepository;

    @Override
    @Cacheable(value = "user_profile", key = "#authentication.name")
    @Transactional(readOnly = true)
    public UserDto getUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.fromUser(user);
    }

    @CachePut(value = "user_profile", key = "#authentication.name")
    @Transactional
    @Override
    public UserDto updateUser(Authentication authentication, UserUpdateRequest updateRequest) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (updateRequest.getFullName() != null) {
            user.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getPhone() != null && !updateRequest.getPhone().isBlank()) {
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

        User saved = userRepository.save(user);
        return userMapper.fromUser(saved);
    }

    @CacheEvict(value = "user_profile", key = "#authentication.name")
    @Transactional
    @Override
    public String updateAvatar(MultipartFile file, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        String oldAvatarUrl = user.getAvatarUrl();

        Map uploadImage = cloudinaryService.uploadImage(file, user.getId());
        String avatarUrl = uploadImage.get("secure_url").toString();
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        // Dọn dẹp ảnh cũ trên Cloudinary chạy nền ngầm không làm chậm luồng người dùng
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank() && !oldAvatarUrl.equals(avatarUrl)) {
            cloudinaryService.deleteImageAsync(oldAvatarUrl);
        }

        return avatarUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::fromUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_profile", allEntries = true)
    public void lockUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        if (user.getRole().equals(Role.ROLE_ADMIN)) {
            throw new IllegalArgumentException("Không thể khóa tài khoản Quản trị viên");
        }

        user.setIsEnabled(false);
        userRepository.save(user);

        // Thu hồi toàn bộ Refresh Token của user bị khóa để buộc logout ngay lập tức
//        refreshTokenRepository.deleteByUserId(user.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "user_profile", allEntries = true)
    public void unlockUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        user.setIsEnabled(true);
        userRepository.save(user);
    }
}
