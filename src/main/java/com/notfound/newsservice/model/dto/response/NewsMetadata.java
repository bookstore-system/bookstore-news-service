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
public class NewsMetadata {

    private String description;

    private List<TableOfContentItem> sections;

    private List<NewsLink> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableOfContentItem {
        private String id;
        private String title;
        private Integer level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsLink {
        private String text;
        private String url;
        private String type;
    }
}
