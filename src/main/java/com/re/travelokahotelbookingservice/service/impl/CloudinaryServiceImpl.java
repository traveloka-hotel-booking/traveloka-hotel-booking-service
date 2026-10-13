package com.re.travelokahotelbookingservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Override
    public Map uploadImage(MultipartFile file, Long userId) {
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        if(file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File tối đa 5MB");
        }

        if(!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ chấp nhận JPG, PNG, WEBP");
        }

        try {
            // Tối ưu hiệu năng: Dùng public_id động kèm timestamp để cache-busting tự nhiên,
            // tắt "invalidate: true" để không bị nghẽn mạng chờ Cloudinary purge toàn bộ CDN edges (tiết kiệm ~1.5s)
//            String publicId = "user-" + userId + "-" + System.currentTimeMillis();
            String publicId = "user-" + userId;

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "traveloka/avatars",
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", false
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }

    @Override
    public Map uploadImage(MultipartFile file, String folder, String publicIdPrefix) {
        if(file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        if(file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File tối đa 5MB");
        }

        if(!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ chấp nhận JPG, PNG, WEBP");
        }

        try {
            String prefix = (publicIdPrefix != null && !publicIdPrefix.isBlank()) ? publicIdPrefix : "img";
            String publicId = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
            String targetFolder = (folder != null && !folder.isBlank()) ? folder : "traveloka";

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", targetFolder,
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", false
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh lên Cloudinary thất bại", e);
        }
    }

    /**
     * Xóa ảnh cũ trên Cloudinary chạy ngầm không giữ luồng của người dùng
     */
    @Async
    @Override
    public void deleteImageAsync(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        // Chỉ xóa nếu ảnh thuộc về Cloudinary của hệ thống
        if (!imageUrl.contains("cloudinary.com") || !imageUrl.contains("traveloka/")) {
            return;
        }

        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return;

            String pathAfterUpload = imageUrl.substring(uploadIndex + "/upload/".length());
            // Loại bỏ version tag nếu có (ví dụ v1728271827/)
            if (pathAfterUpload.startsWith("v") && pathAfterUpload.contains("/")) {
                pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf("/") + 1);
            }

            // Loại bỏ phần mở rộng tệp (.jpg, .png, .webp)
            int dotIndex = pathAfterUpload.lastIndexOf('.');
            String publicId = (dotIndex != -1) ? pathAfterUpload.substring(0, dotIndex) : pathAfterUpload;

            log.info("Thực thi xóa ảnh cũ trên Cloudinary ngầm (Async): publicId={}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Không thể xóa ảnh cũ trên Cloudinary: {}", e.getMessage());
        }
    }
}

