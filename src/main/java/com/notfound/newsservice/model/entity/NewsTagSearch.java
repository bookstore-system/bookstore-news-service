package com.notfound.newsservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "news_tag_searches",
        indexes = {
                @Index(name = "idx_news_tag_searches_tag", columnList = "normalized_tag"),
                @Index(name = "idx_news_tag_searches_user_tag_time", columnList = "user_id, normalized_tag, searched_at"),
                @Index(name = "idx_news_tag_searches_guest_tag_time", columnList = "guest_session_id, normalized_tag, searched_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsTagSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(name = "normalized_tag", nullable = false, length = 100)
    private String normalizedTag;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "guest_session_id", length = 100)
    private String guestSessionId;

    @CreationTimestamp
    @Column(name = "searched_at", nullable = false, updatable = false)
    private LocalDateTime searchedAt;
}
