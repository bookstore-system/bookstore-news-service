package com.notfound.newsservice.repository;

import com.notfound.newsservice.model.entity.NewsTagSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NewsTagSearchRepository extends JpaRepository<NewsTagSearch, Long> {

    boolean existsByUserIdAndNormalizedTagAndSearchedAtAfter(UUID userId, String normalizedTag, LocalDateTime since);

    boolean existsByGuestSessionIdAndNormalizedTagAndSearchedAtAfter(
            String guestSessionId,
            String normalizedTag,
            LocalDateTime since
    );

    @Query("""
            SELECT s.tag AS tag, s.normalizedTag AS normalizedTag, COUNT(s) AS searchCount
            FROM NewsTagSearch s
            GROUP BY s.tag, s.normalizedTag
            ORDER BY COUNT(s) DESC, MAX(s.searchedAt) DESC
            """)
    List<PopularNewsTagProjection> findPopularTags(Pageable pageable);
}
