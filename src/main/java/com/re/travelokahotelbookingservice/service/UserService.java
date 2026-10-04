package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserDto getUser(Authentication authentication);
    UserDto updateUser(Authentication authentication, UserUpdateRequest updateRequest);
    String updateAvatar(MultipartFile file, Authentication authentication);
    Page<UserDto> getAllUsers(int page, int size);
    void lockUser(Long id);
    void unlockUser(Long id);
}
