package com.notfound.newsservice.model.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNewsRequest {

    private String title;
    private String content;
    private String summary;
    private String category;
    private List<String> tags;
    private Boolean featured;
    private String status;
    private String metadata;

    @Valid
    private List<NewsImageRequest> images;
}
