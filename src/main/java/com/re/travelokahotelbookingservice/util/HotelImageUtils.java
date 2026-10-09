package com.re.travelokahotelbookingservice.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.re.travelokahotelbookingservice.entity.Hotel;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility chuyên trách xử lý và trích xuất danh sách ảnh của Khách sạn (tuân thủ SRP).
 * Tách biệt hoàn toàn khỏi tầng Entity và sử dụng ObjectMapper static để tối ưu hiệu năng ánh xạ.
 */
@Slf4j
public final class HotelImageUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE_REF = new TypeReference<>() {};

    private HotelImageUtils() {
        // Private constructor cho utility class
    }

    /**
     * Trích xuất danh sách hình ảnh từ đối tượng Hotel
     */
    public static List<String> extractImageList(Hotel hotel) {
        if (hotel == null) {
            return Collections.emptyList();
        }
        return parseImageList(hotel.getImages(), hotel.getThumbnailUrl());
    }

    /**
     * Parse chuỗi lưu trữ hình ảnh (JSON array, delimiter ;;;, |||, \n, hoặc dấu phẩy) thành List<String>
     */
    public static List<String> parseImageList(String images, String thumbnailUrl) {
        if (images != null && !images.isBlank()) {
            String trimmed = images.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                try {
                    List<String> list = OBJECT_MAPPER.readValue(trimmed, LIST_TYPE_REF);
                    if (list != null && !list.isEmpty()) {
                        return list.stream()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                    }
                } catch (Exception ignored) {
                    log.debug("Không thể parse images dạng JSON array: {}", trimmed);
                }
            }
            if (trimmed.contains(";;;")) {
                return Arrays.stream(trimmed.split(";;;"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            if (trimmed.contains("|||")) {
                return Arrays.stream(trimmed.split("\\|\\|\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            if (trimmed.contains("\n")) {
                return Arrays.stream(trimmed.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            return List.of(thumbnailUrl.trim());
        }
        return Collections.emptyList();
    }
}
