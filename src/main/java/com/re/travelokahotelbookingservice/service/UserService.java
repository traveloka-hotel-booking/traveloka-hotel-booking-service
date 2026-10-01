package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.ProfileUpdateRequestDto;
import com.re.travelokahotelbookingservice.dto.response.UserProfileResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileResponseDto getCurrentUserProfile(String username);
    UserProfileResponseDto updateProfile(String username, ProfileUpdateRequestDto request, MultipartFile avatar);
}
