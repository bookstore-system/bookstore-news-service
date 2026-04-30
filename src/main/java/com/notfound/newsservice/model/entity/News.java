package com.notfound.newsservice.model.entity;

import com.notfound.newsservice.model.converter.StringListConverter;
import com.notfound.newsservice.model.enums.NewsStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "news",
        indexes = {
                @Index(name = "idx_news_status", columnList = "status"),
                @Index(name = "idx_news_category", columnList = "category"),
                @Index(name = "idx_news_author", columnList = "author_id"),
                @Index(name = "idx_news_featured", columnList = "featured"),
                @Index(name = "idx_news_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"images"})
@EntityListeners(AuditingEntityListener.class)
public class News {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false, length = 100)
    private String category;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "tags_searchable", columnDefinition = "TEXT")
    private String tagsSearchable;

    @Column(nullable = false)
    @Builder.Default
    private Long views = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NewsStatus status = NewsStatus.DRAFT;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<NewsImage> images = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void rebuildTagsSearchable() {
        if (tags == null || tags.isEmpty()) {
            tagsSearchable = "";
        } else {
            tagsSearchable = String.join(" ", tags);
        }
    }
}
