package com.re.travelokahotelbookingservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CloudinaryService {
    Map uploadImage(MultipartFile file, Long userId);
    Map uploadImage(MultipartFile file, String folder, String publicIdPrefix);
}

