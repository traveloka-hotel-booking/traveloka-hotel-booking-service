package com.re.travelokahotelbookingservice.mapper;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.request.UserRegisterRequest;
import com.re.travelokahotelbookingservice.dto.response.UserRegisterResponse;
import com.re.travelokahotelbookingservice.entity.User;

public interface UserMapper {
    User toUserRegisterRequest(UserRegisterRequest request, String passwordEncoder);
    UserRegisterResponse toUser(User user);
    UserDto toUserLogin(Object principal);
}
