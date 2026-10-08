package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.request.RoomRequestDto;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.RoomResponseDto;
import com.re.travelokahotelbookingservice.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponseDto>>> getRooms(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String status
    ) {
        List<RoomResponseDto> result = roomService.getAllRooms(hotelId, status);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponseDto>> getRoomById(@PathVariable Long id) {
        RoomResponseDto result = roomService.getRoomById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponseDto>> createRoom(@Valid @RequestBody RoomRequestDto request) {
        RoomResponseDto result = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "CREATED", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponseDto>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequestDto request
    ) {
        RoomResponseDto result = roomService.updateRoom(id, request);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "Đã xóa loại phòng thành công"));
    }

    @PutMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<RoomResponseDto>> updateRoomImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        RoomResponseDto result = roomService.updateRoomImage(id, file);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "Cập nhật ảnh loại phòng thành công", result));
    }

    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> uploadRoomImageOnly(@RequestParam("file") MultipartFile file) {
        String url = roomService.uploadRoomImage(file);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "Upload ảnh loại phòng thành công", url));
    }
}

