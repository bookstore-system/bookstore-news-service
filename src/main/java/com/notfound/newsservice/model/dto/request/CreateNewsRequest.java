package com.notfound.newsservice.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewsRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private String summary;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    private List<String> tags;

    private Boolean featured;

    private String status;

    private String metadata;

    @Valid
    private List<NewsImageRequest> images;
}
