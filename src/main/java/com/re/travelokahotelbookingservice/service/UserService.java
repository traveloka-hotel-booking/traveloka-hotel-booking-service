package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface UserService {
    UserDto getUser(Authentication authentication);
    UserDto updateUser(Authentication authentication, UserUpdateRequest updateRequest);
}
