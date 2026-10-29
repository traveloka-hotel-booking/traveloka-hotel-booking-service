package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.UserDto;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserDto> userDtos = userService.getAllUsers(page, size);
        ApiResponse<Page<UserDto>> apiResponse = new ApiResponse<>(200, "SUCCESS", userDtos);
        return ResponseEntity.ok().body(apiResponse);
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<?>> lockUser(@PathVariable Long id) {
        userService.lockUser(id);
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", "Khóa tài khoản thành công");
        return ResponseEntity.ok().body(apiResponse);
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<?>> unlockUser(@PathVariable Long id) {
        userService.unlockUser(id);
        ApiResponse<?> apiResponse = new ApiResponse<>(200, "SUCCESS", "Mở khóa tài khoản thành công");
        return ResponseEntity.ok().body(apiResponse);
    }
}
