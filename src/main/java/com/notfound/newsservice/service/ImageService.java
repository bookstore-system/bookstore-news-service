package com.notfound.newsservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ImageService {

    Map<String, Object> uploadImage(MultipartFile file, String folder);

    List<Map<String, Object>> uploadMultipleImages(List<MultipartFile> files, String folder);

    void deleteImage(String url);

    boolean isConfigured();
}
