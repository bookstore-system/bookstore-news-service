package com.notfound.newsservice.repository;

import com.notfound.newsservice.model.entity.News;
import com.notfound.newsservice.model.enums.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {

    Page<News> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<News> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<News> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title, Pageable pageable);

    Page<News> findByStatusOrderByCreatedAtDesc(NewsStatus status, Pageable pageable);

    Page<News> findByStatus(NewsStatus status, Pageable pageable);

    Page<News> findByCategory(String category, Pageable pageable);

    Page<News> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    Page<News> findByCategoryAndStatus(String category, NewsStatus status, Pageable pageable);

    Page<News> findByCategoryAndStatusOrderByCreatedAtDesc(String category, NewsStatus status, Pageable pageable);

    Page<News> findByFeaturedOrderByViewsDesc(Boolean featured, Pageable pageable);

    Page<News> findByStatusAndFeaturedOrderByCreatedAtDesc(NewsStatus status, Boolean featured, Pageable pageable);

    Page<News> findAllByOrderByViewsDesc(Pageable pageable);

    long countByStatus(NewsStatus status);

    long countByFeaturedTrue();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(n.views), 0) FROM News n")
    Long sumAllViews();

    @Query("SELECT COALESCE(SUM(n.views), 0) FROM News n WHERE n.createdAt BETWEEN :start AND :end")
    Long sumViewsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT n.category, COUNT(n) FROM News n GROUP BY n.category ORDER BY COUNT(n) DESC")
    List<Object[]> countByCategoryGroup();

    @Query(value = "SELECT FUNCTION('DATE', n.createdAt), COALESCE(SUM(n.views), 0), COUNT(n) " +
            "FROM News n WHERE n.createdAt BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('DATE', n.createdAt) " +
            "ORDER BY FUNCTION('DATE', n.createdAt)")
    List<Object[]> getViewsTrendBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT n FROM News n " +
            "WHERE LOWER(n.tagsSearchable) LIKE LOWER(CONCAT('%', :tag, '%')) " +
            "AND n.status = com.notfound.newsservice.model.enums.NewsStatus.PUBLISHED")
    List<News> findByTag(@Param("tag") String tag);

    @Query(value = "SELECT n FROM News n WHERE " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(n.tagsSearchable) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND n.status = com.notfound.newsservice.model.enums.NewsStatus.PUBLISHED",
            countQuery = "SELECT COUNT(n) FROM News n WHERE " +
                    "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    " OR LOWER(n.tagsSearchable) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND n.status = com.notfound.newsservice.model.enums.NewsStatus.PUBLISHED")
    Page<News> searchByTitleOrTags(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM News n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<News> searchByTitleForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
