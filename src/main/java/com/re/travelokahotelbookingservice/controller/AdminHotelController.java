package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.hotel.HotelRequestDto;
import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.dto.response.HotelResponseDto;
import com.re.travelokahotelbookingservice.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1/admin/hotels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHotelController {

    private final HotelService hotelService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HotelResponseDto>>> getHotels(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<HotelResponseDto> result = hotelService.getAllHotels(search, city, pageable);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<HotelResponseDto>>> getAllHotelsList() {
        List<HotelResponseDto> result = hotelService.getAllHotelsList();
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponseDto>> getHotelById(@PathVariable Long id) {
        HotelResponseDto result = hotelService.getHotelById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HotelResponseDto>> createHotel(@Valid @RequestBody HotelRequestDto request) {
        HotelResponseDto result = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "CREATED", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponseDto>> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody HotelRequestDto request
    ) {
        HotelResponseDto result = hotelService.updateHotel(id, request);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "Đã xóa khách sạn thành công"));
    }

    @PutMapping(value = "/{id}/thumbnail", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<HotelResponseDto>> updateThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        HotelResponseDto result = hotelService.updateThumbnail(id, file);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "Cập nhật ảnh đại diện khách sạn thành công", result));
    }

    @PostMapping(value = "/upload-thumbnail", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> uploadThumbnailOnly(@RequestParam("file") MultipartFile file) {
        String url = hotelService.uploadHotelImage(file);
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "Upload ảnh thành công", url));
    }
}

