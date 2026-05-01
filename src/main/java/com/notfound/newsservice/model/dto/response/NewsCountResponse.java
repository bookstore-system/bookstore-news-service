package com.notfound.newsservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCountResponse {
    private long totalDraft;
    private long totalPublished;
    private long totalArchived;
    private long total;
}
