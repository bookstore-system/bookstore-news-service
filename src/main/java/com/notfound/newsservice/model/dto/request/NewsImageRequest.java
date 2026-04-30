package com.notfound.newsservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsImageRequest {

    @NotBlank(message = "URL ảnh không được để trống")
    private String url;

    private Integer priority;
}
