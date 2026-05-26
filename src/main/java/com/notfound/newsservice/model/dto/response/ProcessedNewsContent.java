package com.notfound.newsservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedNewsContent {
    private String htmlContent;
    private String metadataJson;
}
