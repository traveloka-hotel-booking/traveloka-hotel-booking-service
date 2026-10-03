package com.re.travelokahotelbookingservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.re.travelokahotelbookingservice.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

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
            String publicId = "user-" + userId;

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "traveloka/avatars",
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }

}
