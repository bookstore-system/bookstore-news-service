package com.notfound.newsservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.notfound.newsservice.exception.BadRequestException;
import com.notfound.newsservice.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageServiceImpl implements ImageService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Override
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank() && !"missing".equals(cloudName);
    }

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh trống");
        }

        if (!isConfigured()) {
            log.warn("Cloudinary chưa cấu hình — trả về URL placeholder cho file {}", file.getOriginalFilename());
            Map<String, Object> placeholder = new HashMap<>();
            String placeholderUrl = "https://placehold.co/800x600?text=" +
                    UUID.randomUUID().toString().substring(0, 8);
            placeholder.put("url", placeholderUrl);
            placeholder.put("public_id", "placeholder/" + UUID.randomUUID());
            placeholder.put("resource_type", "image");
            placeholder.put("placeholder", true);
            return placeholder;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );
            log.info("Đã upload ảnh lên Cloudinary: public_id={}", result.get("public_id"));
            return result;
        } catch (IOException e) {
            log.error("Lỗi upload ảnh lên Cloudinary", e);
            throw new BadRequestException("Không thể upload ảnh: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> uploadMultipleImages(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Danh sách file rỗng");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadImage(file, folder));
        }
        return results;
    }

    @Override
    public void deleteImage(String url) {
        if (url == null || url.isBlank()) return;

        if (!isConfigured()) {
            log.warn("Cloudinary chưa cấu hình — bỏ qua delete cho url={}", url);
            return;
        }

        try {
            String publicId = extractPublicId(url);
            if (publicId == null) {
                log.warn("Không thể trích xuất publicId từ url={}", url);
                return;
            }
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Đã xoá ảnh khỏi Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Lỗi delete ảnh Cloudinary url={}", url, e);
        }
    }

    private String extractPublicId(String url) {
        if (url == null) return null;
        int uploadIdx = url.indexOf("/upload/");
        if (uploadIdx < 0) return null;
        String afterUpload = url.substring(uploadIdx + "/upload/".length());
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
        }
        int dotIdx = afterUpload.lastIndexOf('.');
        return dotIdx > 0 ? afterUpload.substring(0, dotIdx) : afterUpload;
    }
}
