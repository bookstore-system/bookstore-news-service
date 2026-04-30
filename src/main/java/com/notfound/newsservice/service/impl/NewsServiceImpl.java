package com.notfound.newsservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.newsservice.exception.BadRequestException;
import com.notfound.newsservice.exception.ForbiddenException;
import com.notfound.newsservice.exception.NewsNotFoundException;
import com.notfound.newsservice.model.dto.request.CreateNewsRequest;
import com.notfound.newsservice.model.dto.request.UpdateNewsRequest;
import com.notfound.newsservice.model.dto.response.NewsImageResponse;
import com.notfound.newsservice.model.dto.response.NewsMetadata;
import com.notfound.newsservice.model.dto.response.NewsResponse;
import com.notfound.newsservice.model.dto.response.NewsStatsResponse;
import com.notfound.newsservice.model.dto.response.ProcessedNewsContent;
import com.notfound.newsservice.model.entity.News;
import com.notfound.newsservice.model.entity.NewsImage;
import com.notfound.newsservice.model.enums.NewsStatus;
import com.notfound.newsservice.repository.NewsImageRepository;
import com.notfound.newsservice.repository.NewsRepository;
import com.notfound.newsservice.service.ImageService;
import com.notfound.newsservice.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsImageRepository newsImageRepository;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NewsResponse createNews(CreateNewsRequest request, UUID authorId, String authorName) {
        log.info("Creating news, title='{}', authorId={}", request.getTitle(), authorId);

        ProcessedNewsContent processed = processContent(request.getContent());

        News news = News.builder()
                .title(request.getTitle())
                .content(processed.getHtmlContent())
                .summary(request.getSummary())
                .category(request.getCategory())
                .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                .views(0L)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .metadata(processed.getMetadataJson())
                .status(parseStatus(request.getStatus(), NewsStatus.DRAFT))
                .authorId(authorId)
                .authorName(authorName != null ? authorName : "User-" + authorId)
                .images(new ArrayList<>())
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<NewsImage> images = request.getImages().stream()
                    .map(req -> NewsImage.builder()
                            .url(req.getUrl())
                            .priority(req.getPriority() != null ? req.getPriority() : 1)
                            .news(news)
                            .build())
                    .collect(Collectors.toList());
            news.getImages().addAll(images);
        }

        News saved = newsRepository.save(news);
        log.info("News tạo thành công, id={}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public NewsResponse updateNews(UUID newsId, UpdateNewsRequest request) {
        log.info("Updating news, id={}", newsId);
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));

        if (request.getTitle() != null) news.setTitle(request.getTitle());
        if (request.getSummary() != null) news.setSummary(request.getSummary());
        if (request.getCategory() != null) news.setCategory(request.getCategory());
        if (request.getTags() != null) news.setTags(new ArrayList<>(request.getTags()));
        if (request.getFeatured() != null) news.setFeatured(request.getFeatured());
        if (request.getStatus() != null) news.setStatus(parseStatus(request.getStatus(), news.getStatus()));

        if (request.getContent() != null) {
            ProcessedNewsContent processed = processContent(request.getContent());
            news.setContent(processed.getHtmlContent());
            news.setMetadata(processed.getMetadataJson());
        }

        if (request.getImages() != null) {
            news.getImages().clear();
            List<NewsImage> newImages = request.getImages().stream()
                    .map(req -> NewsImage.builder()
                            .url(req.getUrl())
                            .priority(req.getPriority() != null ? req.getPriority() : 1)
                            .news(news)
                            .build())
                    .collect(Collectors.toList());
            news.getImages().addAll(newImages);
        }

        News updated = newsRepository.save(news);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public NewsResponse getNewsById(UUID newsId) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        news.setViews(news.getViews() + 1);
        News saved = newsRepository.save(news);
        return mapToResponse(saved);
    }

    @Override
    public Page<NewsResponse> getAllNews(Pageable pageable) {
        return newsRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> getPublishedNews(Pageable pageable) {
        return newsRepository.findByStatusOrderByCreatedAtDesc(NewsStatus.PUBLISHED, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> getNewsByAuthor(UUID authorId, Pageable pageable) {
        return newsRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> searchNewsByTitle(String title, Pageable pageable) {
        return newsRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(title, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteNews(UUID newsId) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        if (news.getImages() != null) {
            for (NewsImage img : news.getImages()) {
                imageService.deleteImage(img.getUrl());
            }
        }
        newsRepository.delete(news);
        log.info("Đã xoá news id={}", newsId);
    }

    @Override
    @Transactional
    public NewsResponse publishNews(UUID newsId) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        news.setStatus(NewsStatus.PUBLISHED);
        return mapToResponse(newsRepository.save(news));
    }

    @Override
    @Transactional
    public NewsResponse archiveNews(UUID newsId) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        news.setStatus(NewsStatus.ARCHIVED);
        return mapToResponse(newsRepository.save(news));
    }

    @Override
    @Transactional
    public NewsResponse restoreNews(UUID newsId) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        if (news.getStatus() != NewsStatus.ARCHIVED) {
            throw new BadRequestException("Chỉ có thể khôi phục tin đã ARCHIVED");
        }
        news.setStatus(NewsStatus.DRAFT);
        return mapToResponse(newsRepository.save(news));
    }

    @Override
    public long countByStatus(NewsStatus status) {
        return newsRepository.countByStatus(status);
    }

    @Override
    public Page<NewsResponse> searchNews(String keyword, String category, NewsStatus status, Pageable pageable) {
        log.info("Advanced search keyword={} category={} status={}", keyword, category, status);
        Page<News> page;

        if (keyword != null && !keyword.isBlank()) {
            page = newsRepository.searchByTitleForAdmin(keyword, pageable);
            if ((category != null && !category.isBlank()) || status != null) {
                List<News> filtered = page.getContent().stream().filter(n -> {
                    boolean okCat = category == null || category.isBlank() || category.equals(n.getCategory());
                    boolean okSt = status == null || status == n.getStatus();
                    return okCat && okSt;
                }).collect(Collectors.toList());
                page = new PageImpl<>(filtered, pageable, filtered.size());
            }
        } else if (category != null && !category.isBlank() && status != null) {
            page = newsRepository.findByCategoryAndStatus(category, status, pageable);
        } else if (category != null && !category.isBlank()) {
            page = newsRepository.findByCategory(category, pageable);
        } else if (status != null) {
            page = newsRepository.findByStatus(status, pageable);
        } else {
            page = newsRepository.findAll(pageable);
        }

        return page.map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> searchNewsByTag(String tag, Pageable pageable) {
        List<News> all = newsRepository.findByTag(tag);
        int from = Math.min((int) pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        List<News> sub = all.subList(from, to);
        Page<News> page = new PageImpl<>(sub, pageable, all.size());
        return page.map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> searchNewsByTitleOrTags(String keyword, Pageable pageable) {
        return newsRepository.searchByTitleOrTags(keyword, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> getNewsByCategory(String category, Pageable pageable) {
        return newsRepository.findByCategoryOrderByCreatedAtDesc(category, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> getNewsByStatus(NewsStatus status, Pageable pageable) {
        return newsRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> getFeaturedNews(Boolean featured, Pageable pageable) {
        return newsRepository.findByFeaturedOrderByViewsDesc(featured, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NewsResponse> getNewsByStatusAndFeatured(NewsStatus status, Boolean featured, Pageable pageable) {
        return newsRepository.findByStatusAndFeaturedOrderByCreatedAtDesc(status, featured, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public NewsResponse uploadNewsImages(UUID newsId, List<MultipartFile> images) {
        News news = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        if (images == null || images.isEmpty()) {
            throw new BadRequestException("Danh sách ảnh trống");
        }

        List<Map<String, Object>> uploadResults =
                imageService.uploadMultipleImages(images, "bookstore/news");

        int priority = 1;
        List<NewsImage> existing = newsImageRepository.findByNewsIdOrderByPriorityAsc(newsId);
        if (!existing.isEmpty()) {
            priority = existing.stream()
                    .mapToInt(i -> i.getPriority() == null ? 0 : i.getPriority())
                    .max().orElse(0) + 1;
        }

        for (Map<String, Object> r : uploadResults) {
            NewsImage img = NewsImage.builder()
                    .url((String) r.get("url"))
                    .priority(priority++)
                    .news(news)
                    .build();
            newsImageRepository.save(img);
        }

        News updated = newsRepository.findById(newsId).orElseThrow(() -> new NewsNotFoundException(newsId));
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteNewsImage(UUID newsId, UUID imageId) {
        if (!newsRepository.existsById(newsId)) {
            throw new NewsNotFoundException(newsId);
        }
        NewsImage image = newsImageRepository.findById(imageId)
                .orElseThrow(() -> new NewsNotFoundException("Không tìm thấy ảnh id=" + imageId));
        if (!image.getNews().getId().equals(newsId)) {
            throw new ForbiddenException("Ảnh này không thuộc về news id=" + newsId);
        }
        if (image.getUrl() != null) {
            imageService.deleteImage(image.getUrl());
        }
        newsImageRepository.delete(image);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsStatsResponse getNewsStatistics() {
        log.info("Tính thống kê news");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1L)
                .toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfMonth.minusDays(1).toLocalDate().atTime(23, 59, 59);
        LocalDateTime last30Days = now.minusDays(30);

        long total = newsRepository.count();
        long published = newsRepository.countByStatus(NewsStatus.PUBLISHED);
        long draft = newsRepository.countByStatus(NewsStatus.DRAFT);
        long archived = newsRepository.countByStatus(NewsStatus.ARCHIVED);
        long featured = newsRepository.countByFeaturedTrue();

        long today = newsRepository.countByCreatedAtBetween(startOfToday, now);
        long week = newsRepository.countByCreatedAtBetween(startOfWeek, now);
        long month = newsRepository.countByCreatedAtBetween(startOfMonth, now);
        long lastMonth = newsRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);

        Long totalViews = newsRepository.sumAllViews();
        if (totalViews == null) totalViews = 0L;
        double avgViews = total > 0 ? (double) totalViews / total : 0.0;

        List<NewsStatsResponse.NewsByCategoryStats> byCategory = newsRepository.countByCategoryGroup().stream()
                .map(row -> {
                    String cat = (String) row[0];
                    long cnt = ((Number) row[1]).longValue();
                    double pct = total > 0 ? (cnt * 100.0) / total : 0.0;
                    return NewsStatsResponse.NewsByCategoryStats.builder()
                            .category(cat)
                            .count(cnt)
                            .percentage(Math.round(pct * 100.0) / 100.0)
                            .build();
                }).collect(Collectors.toList());

        List<NewsStatsResponse.TopViewedNews> topViewed = newsRepository
                .findAllByOrderByViewsDesc(PageRequest.of(0, 10)).getContent().stream()
                .map(n -> NewsStatsResponse.TopViewedNews.builder()
                        .id(n.getId().toString())
                        .title(n.getTitle())
                        .views(n.getViews())
                        .category(n.getCategory())
                        .publishedAt(n.getCreatedAt() == null ? null : n.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        List<NewsStatsResponse.ViewsTrendData> trend = newsRepository.getViewsTrendBetween(last30Days, now).stream()
                .map(row -> {
                    Object dateObj = row[0];
                    String dateStr = dateObj == null ? "" : dateObj.toString();
                    long views = ((Number) row[1]).longValue();
                    long cnt = ((Number) row[2]).longValue();
                    return NewsStatsResponse.ViewsTrendData.builder()
                            .date(dateStr)
                            .views(views)
                            .newsCount(cnt)
                            .build();
                }).collect(Collectors.toList());

        double newsGrowth;
        if (lastMonth > 0) {
            newsGrowth = ((double) (month - lastMonth) / lastMonth) * 100;
        } else {
            newsGrowth = month > 0 ? 100.0 : 0.0;
        }
        newsGrowth = Math.round(newsGrowth * 100.0) / 100.0;

        Long viewsThisMonth = newsRepository.sumViewsBetween(startOfMonth, now);
        Long viewsLastMonth = newsRepository.sumViewsBetween(startOfLastMonth, endOfLastMonth);
        if (viewsThisMonth == null) viewsThisMonth = 0L;
        if (viewsLastMonth == null) viewsLastMonth = 0L;

        double viewsGrowth;
        if (viewsLastMonth > 0) {
            viewsGrowth = ((double) (viewsThisMonth - viewsLastMonth) / viewsLastMonth) * 100;
        } else {
            viewsGrowth = viewsThisMonth > 0 ? 100.0 : 0.0;
        }
        viewsGrowth = Math.round(viewsGrowth * 100.0) / 100.0;

        return NewsStatsResponse.builder()
                .totalNews(total)
                .publishedNews(published)
                .draftNews(draft)
                .archivedNews(archived)
                .featuredNews(featured)
                .newNewsThisMonth(month)
                .newNewsThisWeek(week)
                .newNewsToday(today)
                .totalViews(totalViews)
                .avgViewsPerNews(Math.round(avgViews * 100.0) / 100.0)
                .totalComments(0L)
                .newsByCategory(byCategory)
                .topViewedNews(topViewed)
                .viewsTrend(trend)
                .newsGrowthPercentage(newsGrowth)
                .viewsGrowthPercentage(viewsGrowth)
                .build();
    }

    private NewsStatus parseStatus(String raw, NewsStatus fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return NewsStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Status không hợp lệ '{}' — dùng fallback {}", raw, fallback);
            return fallback;
        }
    }

    private ProcessedNewsContent processContent(String html) {
        if (html == null || html.isBlank()) {
            return new ProcessedNewsContent(html, "{}");
        }
        try {
            Document doc = Jsoup.parse(html);

            List<NewsMetadata.TableOfContentItem> sections = new ArrayList<>();
            Elements headings = doc.select("h2, h3, h4");
            int idx = 1;
            for (Element h : headings) {
                String id = h.attr("id");
                if (id == null || id.isEmpty()) {
                    id = "section-" + idx++;
                    h.attr("id", id);
                }
                int level = Integer.parseInt(h.tagName().substring(1));
                sections.add(NewsMetadata.TableOfContentItem.builder()
                        .id(id).title(h.text()).level(level).build());
            }

            List<NewsMetadata.NewsLink> links = new ArrayList<>();
            for (Element a : doc.select("a[href]")) {
                String url = a.attr("href");
                String type = "external";
                if (url.startsWith("/books/")) type = "book";
                else if (url.startsWith("/products/")) type = "product";
                else if (url.startsWith("/")) type = "internal";
                links.add(NewsMetadata.NewsLink.builder().text(a.text()).url(url).type(type).build());
            }

            String description = "";
            Element first = doc.selectFirst("p");
            if (first != null) {
                description = first.text();
                if (description.length() > 200) description = description.substring(0, 197) + "...";
            }

            NewsMetadata md = NewsMetadata.builder()
                    .description(description).sections(sections).links(links).build();

            String htmlOut = doc.body() != null ? doc.body().html() : html;
            return new ProcessedNewsContent(htmlOut, objectMapper.writeValueAsString(md));
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize metadata", e);
            return new ProcessedNewsContent(html, "{}");
        }
    }

    private NewsResponse mapToResponse(News n) {
        NewsMetadata md = null;
        if (n.getMetadata() != null && !n.getMetadata().isBlank() && !"{}".equals(n.getMetadata())) {
            try {
                md = objectMapper.readValue(n.getMetadata(), NewsMetadata.class);
            } catch (JsonProcessingException e) {
                log.warn("Không parse được metadata cho news id={}: {}", n.getId(), e.getMessage());
            }
        }

        List<NewsImageResponse> imageResponses = n.getImages() == null
                ? new ArrayList<>()
                : n.getImages().stream()
                .sorted((a, b) -> {
                    int pa = a.getPriority() == null ? 0 : a.getPriority();
                    int pb = b.getPriority() == null ? 0 : b.getPriority();
                    return Integer.compare(pa, pb);
                })
                .map(img -> NewsImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .priority(img.getPriority())
                        .uploadedAt(img.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        return NewsResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .summary(n.getSummary())
                .content(n.getContent())
                .metadata(md)
                .status(n.getStatus().name())
                .category(n.getCategory())
                .tags(n.getTags())
                .views(n.getViews())
                .featured(n.getFeatured())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .publishedAt(n.getStatus() == NewsStatus.PUBLISHED ? n.getUpdatedAt() : null)
                .authorId(n.getAuthorId())
                .authorName(n.getAuthorName())
                .images(imageResponses)
                .build();
    }
}
