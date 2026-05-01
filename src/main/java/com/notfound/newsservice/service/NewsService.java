package com.notfound.newsservice.service;

import com.notfound.newsservice.model.dto.request.CreateNewsRequest;
import com.notfound.newsservice.model.dto.request.UpdateNewsRequest;
import com.notfound.newsservice.model.dto.response.NewsResponse;
import com.notfound.newsservice.model.dto.response.NewsStatsResponse;
import com.notfound.newsservice.model.enums.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface NewsService {

    NewsResponse createNews(CreateNewsRequest request, UUID authorId, String authorName);

    NewsResponse updateNews(UUID newsId, UpdateNewsRequest request);

    NewsResponse getNewsById(UUID newsId);

    Page<NewsResponse> getAllNews(Pageable pageable);

    Page<NewsResponse> getPublishedNews(Pageable pageable);

    Page<NewsResponse> getNewsByAuthor(UUID authorId, Pageable pageable);

    Page<NewsResponse> searchNewsByTitle(String title, Pageable pageable);

    void deleteNews(UUID newsId);

    NewsResponse publishNews(UUID newsId);

    NewsResponse archiveNews(UUID newsId);

    NewsResponse restoreNews(UUID newsId);

    long countByStatus(NewsStatus status);

    Page<NewsResponse> searchNews(String keyword, String category, NewsStatus status, Pageable pageable);

    Page<NewsResponse> searchNewsByTag(String tag, Pageable pageable);

    Page<NewsResponse> searchNewsByTitleOrTags(String keyword, Pageable pageable);

    Page<NewsResponse> getNewsByCategory(String category, Pageable pageable);

    Page<NewsResponse> getNewsByStatus(NewsStatus status, Pageable pageable);

    Page<NewsResponse> getFeaturedNews(Boolean featured, Pageable pageable);

    Page<NewsResponse> getNewsByStatusAndFeatured(NewsStatus status, Boolean featured, Pageable pageable);

    NewsResponse uploadNewsImages(UUID newsId, List<MultipartFile> images);

    void deleteNewsImage(UUID newsId, UUID imageId);

    NewsStatsResponse getNewsStatistics();
}
