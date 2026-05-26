package com.notfound.newsservice.controller;

import com.notfound.newsservice.model.dto.request.CreateNewsRequest;
import com.notfound.newsservice.model.dto.request.NewsTagSearchRequest;
import com.notfound.newsservice.model.dto.request.UpdateNewsRequest;
import com.notfound.newsservice.model.dto.response.ApiResponse;
import com.notfound.newsservice.model.dto.response.NewsCountResponse;
import com.notfound.newsservice.model.dto.response.NewsResponse;
import com.notfound.newsservice.model.dto.response.NewsStatsResponse;
import com.notfound.newsservice.model.dto.response.PopularNewsTagResponse;
import com.notfound.newsservice.model.enums.NewsStatus;
import com.notfound.newsservice.service.NewsService;
import com.notfound.newsservice.config.OpenApiConfig;
import com.notfound.newsservice.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tin tức", description = "Guest: GET /published, GET /{id} (PUBLISHED). Admin: CRUD, thống kê, tìm kiếm — cần X-User-Role ADMIN")
public class NewsController {

    private final NewsService newsService;
    private final UserContext userContext;

    @GetMapping
    @Operation(summary = "Danh sách tin (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Page<NewsResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NewsStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        userContext.requireAdmin();
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
    @Operation(summary = "Tin đã xuất bản (Guest) — lọc keyword, category, tag, sort")
    public ApiResponse<Page<NewsResponse>> getPublished(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NewsResponse> result = newsService.searchPublishedNews(keyword, category, tag, pageable);
        return ApiResponse.success("Lấy tin đã xuất bản", result);
    }

    @PostMapping("/tag-searches")
    @Operation(summary = "Ghi nhận lượt tìm theo tag (Guest/User)")
    public ApiResponse<Void> recordTagSearch(
            @Valid @RequestBody NewsTagSearchRequest request,
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId
    ) {
        newsService.recordTagSearch(request, userContext.getOptionalUserId(), guestSessionId);
        return ApiResponse.success("Đã ghi nhận lượt tìm theo tag", null);
    }

    @GetMapping("/popular-tags")
    @Operation(summary = "Lấy tags phổ biến từ search thật (Guest)")
    public ApiResponse<List<PopularNewsTagResponse>> getPopularTags(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ApiResponse.success("Lấy tags phổ biến thành công", newsService.getPopularTags(limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết tin — Guest chỉ PUBLISHED; Admin xem mọi trạng thái")
    public ApiResponse<NewsResponse> getById(@PathVariable UUID id) {
        NewsResponse response = userContext.isAdmin()
                ? newsService.getNewsById(id)
                : newsService.getPublishedNewsById(id);
        return ApiResponse.success("Lấy chi tiết tin tức", response);
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm tin theo tiêu đề (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Page<NewsResponse>> search(@RequestParam String title,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        userContext.requireAdmin();
        return ApiResponse.success("Tìm kiếm tin tức",
                newsService.searchNewsByTitle(title, PageRequest.of(page, size)));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Tin theo tác giả (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Page<NewsResponse>> getByAuthor(@PathVariable UUID authorId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        userContext.requireAdmin();
        return ApiResponse.success("Lấy tin của tác giả",
                newsService.getNewsByAuthor(authorId, PageRequest.of(page, size)));
    }

    @GetMapping("/my-news")
    @Operation(summary = "Tin của tôi (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ID)
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Page<NewsResponse>> myNews(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        userContext.requireAdmin();
        UUID userId = userContext.requireUserId();
        return ApiResponse.success("Lấy tin của bạn",
                newsService.getNewsByAuthor(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Tin theo danh mục (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Page<NewsResponse>> getByCategory(@PathVariable String category,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        userContext.requireAdmin();
        return ApiResponse.success("Lấy tin theo danh mục",
                newsService.getNewsByCategory(category, PageRequest.of(page, size)));
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Tin theo tag (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Page<NewsResponse>> getByTag(@PathVariable String tag,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        userContext.requireAdmin();
        return ApiResponse.success("Lấy tin theo tag",
                newsService.searchNewsByTag(tag, PageRequest.of(page, size)));
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Tìm kiếm nâng cao (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
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
        userContext.requireAdmin();
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NewsResponse> result = (tag != null && !tag.isBlank())
                ? newsService.searchNewsByTag(tag, pageable)
                : newsService.searchNews(keyword, category, status, pageable);
        return ApiResponse.success("Tìm kiếm nâng cao", result);
    }

    @PostMapping
    @Operation(
            summary = "Tạo tin (Admin)",
            description = "Cần `X-User-Id` (UUID), `X-User-Role: ADMIN` hoặc `ROLE_ADMIN`, tùy chọn `X-User-Name`. Dùng Authorize trên Swagger UI."
    )
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ID)
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsResponse> createNews(@Valid @RequestBody CreateNewsRequest request) {
        UUID userId = userContext.requireUserId();
        userContext.requireAdmin();
        String authorName = userContext.getUserName();
        log.info("User {} đang tạo news: {}", userId, request.getTitle());
        return ApiResponse.success("Tạo tin tức thành công",
                newsService.createNews(request, userId, authorName));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tin (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsResponse> updateNews(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateNewsRequest request) {
        userContext.requireAdmin();
        return ApiResponse.success("Cập nhật tin tức thành công",
                newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tin (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Void> deleteNews(@PathVariable UUID id) {
        userContext.requireAdmin();
        newsService.deleteNews(id);
        return ApiResponse.success("Xoá tin tức thành công", null);
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Xuất bản (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsResponse> publish(@PathVariable UUID id) {
        userContext.requireAdmin();
        return ApiResponse.success("Xuất bản tin tức thành công",
                newsService.publishNews(id));
    }

    @PutMapping("/{id}/archive")
    @Operation(summary = "Lưu trữ (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsResponse> archive(@PathVariable UUID id) {
        userContext.requireAdmin();
        return ApiResponse.success("Lưu trữ tin tức thành công",
                newsService.archiveNews(id));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Khôi phục (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsResponse> restore(@PathVariable UUID id) {
        userContext.requireAdmin();
        return ApiResponse.success("Khôi phục tin tức thành công",
                newsService.restoreNews(id));
    }

    @GetMapping("/stats/count")
    @Operation(summary = "Đếm tin theo trạng thái (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsCountResponse> getCount() {
        userContext.requireAdmin();
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
    @Operation(summary = "Thống kê chi tiết (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsStatsResponse> getStatistics() {
        userContext.requireAdmin();
        return ApiResponse.success("Lấy thống kê tin tức",
                newsService.getNewsStatistics());
    }

    @PostMapping("/{newsId}/images")
    @Operation(summary = "Upload ảnh tin (Admin, multipart)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<NewsResponse> uploadImages(@PathVariable UUID newsId,
                                                  @RequestParam("images") List<MultipartFile> images) {
        userContext.requireAdmin();
        return ApiResponse.success("Upload ảnh cho tin tức thành công",
                newsService.uploadNewsImages(newsId, images));
    }

    @DeleteMapping("/{newsId}/images/{imageId}")
    @Operation(summary = "Xóa ảnh tin (Admin)")
    @SecurityRequirement(name = OpenApiConfig.HEADER_USER_ROLE)
    public ApiResponse<Void> deleteImage(@PathVariable UUID newsId,
                                         @PathVariable Long imageId) {
        userContext.requireAdmin();
        newsService.deleteNewsImage(newsId, imageId);
        return ApiResponse.success("Xoá ảnh thành công", null);
    }
}
