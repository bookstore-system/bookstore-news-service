package com.notfound.newsservice.controller;

import com.notfound.newsservice.model.dto.request.CreateNewsRequest;
import com.notfound.newsservice.model.dto.request.UpdateNewsRequest;
import com.notfound.newsservice.model.dto.response.ApiResponse;
import com.notfound.newsservice.model.dto.response.NewsCountResponse;
import com.notfound.newsservice.model.dto.response.NewsResponse;
import com.notfound.newsservice.model.dto.response.NewsStatsResponse;
import com.notfound.newsservice.model.enums.NewsStatus;
import com.notfound.newsservice.service.NewsService;
import com.notfound.newsservice.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;
    private final UserContext userContext;

    @GetMapping
    public ApiResponse<Page<NewsResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NewsStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        Page<NewsResponse> news;
        if (status != null && featured != null) {
            news = newsService.getNewsByStatusAndFeatured(status, featured, pageable);
        } else if (status != null) {
            news = newsService.getNewsByStatus(status, pageable);
        } else if (featured != null) {
            news = newsService.getFeaturedNews(featured, pageable);
        } else {
            news = newsService.getAllNews(pageable);
        }
        return ApiResponse.success("Lấy danh sách tin tức thành công", news);
    }

    @GetMapping("/published")
    public ApiResponse<Page<NewsResponse>> getPublished(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Lấy tin đã xuất bản",
                newsService.getPublishedNews(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success("Lấy chi tiết tin tức",
                newsService.getNewsById(id));
    }

    @GetMapping("/search")
    public ApiResponse<Page<NewsResponse>> search(@RequestParam String title,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Tìm kiếm tin tức",
                newsService.searchNewsByTitle(title, PageRequest.of(page, size)));
    }

    @GetMapping("/author/{authorId}")
    public ApiResponse<Page<NewsResponse>> getByAuthor(@PathVariable UUID authorId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Lấy tin của tác giả",
                newsService.getNewsByAuthor(authorId, PageRequest.of(page, size)));
    }

    @GetMapping("/my-news")
    public ApiResponse<Page<NewsResponse>> myNews(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        UUID userId = userContext.requireUserId();
        return ApiResponse.success("Lấy tin của bạn",
                newsService.getNewsByAuthor(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/category/{category}")
    public ApiResponse<Page<NewsResponse>> getByCategory(@PathVariable String category,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Lấy tin theo danh mục",
                newsService.getNewsByCategory(category, PageRequest.of(page, size)));
    }

    @GetMapping("/tag/{tag}")
    public ApiResponse<Page<NewsResponse>> getByTag(@PathVariable String tag,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Lấy tin theo tag",
                newsService.searchNewsByTag(tag, PageRequest.of(page, size)));
    }

    @GetMapping("/advanced-search")
    public ApiResponse<Page<NewsResponse>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) NewsStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NewsResponse> result = (tag != null && !tag.isBlank())
                ? newsService.searchNewsByTag(tag, pageable)
                : newsService.searchNews(keyword, category, status, pageable);
        return ApiResponse.success("Tìm kiếm nâng cao", result);
    }

    @PostMapping
    public ApiResponse<NewsResponse> createNews(@Valid @RequestBody CreateNewsRequest request) {
        UUID userId = userContext.requireUserId();
        userContext.requireAdmin();
        String authorName = userContext.getUserName();
        log.info("User {} đang tạo news: {}", userId, request.getTitle());
        return ApiResponse.success("Tạo tin tức thành công",
                newsService.createNews(request, userId, authorName));
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsResponse> updateNews(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateNewsRequest request) {
        userContext.requireAdmin();
        return ApiResponse.success("Cập nhật tin tức thành công",
                newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNews(@PathVariable UUID id) {
        userContext.requireAdmin();
        newsService.deleteNews(id);
        return ApiResponse.success("Xoá tin tức thành công", null);
    }

    @PutMapping("/{id}/publish")
    public ApiResponse<NewsResponse> publish(@PathVariable UUID id) {
        userContext.requireAdmin();
        return ApiResponse.success("Xuất bản tin tức thành công",
                newsService.publishNews(id));
    }

    @PutMapping("/{id}/archive")
    public ApiResponse<NewsResponse> archive(@PathVariable UUID id) {
        userContext.requireAdmin();
        return ApiResponse.success("Lưu trữ tin tức thành công",
                newsService.archiveNews(id));
    }

    @PutMapping("/{id}/restore")
    public ApiResponse<NewsResponse> restore(@PathVariable UUID id) {
        userContext.requireAdmin();
        return ApiResponse.success("Khôi phục tin tức thành công",
                newsService.restoreNews(id));
    }

    @GetMapping("/stats/count")
    public ApiResponse<NewsCountResponse> getCount() {
        long draft = newsService.countByStatus(NewsStatus.DRAFT);
        long published = newsService.countByStatus(NewsStatus.PUBLISHED);
        long archived = newsService.countByStatus(NewsStatus.ARCHIVED);
        NewsCountResponse stats = NewsCountResponse.builder()
                .totalDraft(draft)
                .totalPublished(published)
                .totalArchived(archived)
                .total(draft + published + archived)
                .build();
        return ApiResponse.success("Đếm số lượng tin theo trạng thái", stats);
    }

    @GetMapping("/statistics")
    public ApiResponse<NewsStatsResponse> getStatistics() {
        userContext.requireAdmin();
        return ApiResponse.success("Lấy thống kê tin tức",
                newsService.getNewsStatistics());
    }

    @PostMapping("/{newsId}/images")
    public ApiResponse<NewsResponse> uploadImages(@PathVariable UUID newsId,
                                                  @RequestParam("images") List<MultipartFile> images) {
        userContext.requireAdmin();
        return ApiResponse.success("Upload ảnh cho tin tức thành công",
                newsService.uploadNewsImages(newsId, images));
    }

    @DeleteMapping("/{newsId}/images/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable UUID newsId,
                                         @PathVariable UUID imageId) {
        userContext.requireAdmin();
        newsService.deleteNewsImage(newsId, imageId);
        return ApiResponse.success("Xoá ảnh thành công", null);
    }
}
