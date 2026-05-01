package com.notfound.newsservice.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsStatsResponse {

    // Tổng quan
    private Long totalNews;
    private Long publishedNews;
    private Long draftNews;
    private Long archivedNews;
    private Long featuredNews;

    // Theo thời gian
    private Long newNewsThisMonth;
    private Long newNewsThisWeek;
    private Long newNewsToday;

    // Tương tác
    private Long totalViews;
    private Double avgViewsPerNews;
    private Long totalComments;

    // Phân tích
    private List<NewsByCategoryStats> newsByCategory;
    private List<TopViewedNews> topViewedNews;
    private List<ViewsTrendData> viewsTrend;

    // Tăng trưởng
    private Double newsGrowthPercentage;
    private Double viewsGrowthPercentage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsByCategoryStats {
        private String category;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopViewedNews {
        private String id;
        private String title;
        private Long views;
        private String category;
        private String publishedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewsTrendData {
        private String date;
        private Long views;
        private Long newsCount;
    }
}
