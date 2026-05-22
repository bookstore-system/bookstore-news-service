package com.notfound.newsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.newsservice.exception.BadRequestException;
import com.notfound.newsservice.exception.ForbiddenException;
import com.notfound.newsservice.exception.NewsNotFoundException;
import com.notfound.newsservice.model.dto.request.CreateNewsRequest;
import com.notfound.newsservice.model.dto.request.NewsImageRequest;
import com.notfound.newsservice.model.dto.request.UpdateNewsRequest;
import com.notfound.newsservice.model.dto.response.NewsResponse;
import com.notfound.newsservice.model.dto.response.NewsStatsResponse;
import com.notfound.newsservice.model.entity.News;
import com.notfound.newsservice.model.entity.NewsImage;
import com.notfound.newsservice.model.enums.NewsStatus;
import com.notfound.newsservice.repository.NewsImageRepository;
import com.notfound.newsservice.repository.NewsRepository;
import com.notfound.newsservice.service.impl.NewsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplUnitTest {

    private static final UUID NEWS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID AUTHOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    NewsRepository newsRepository;
    @Mock
    NewsImageRepository newsImageRepository;
    @Mock
    ImageService imageService;

    NewsServiceImpl newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsServiceImpl(
                newsRepository,
                newsImageRepository,
                imageService,
                new ObjectMapper()
        );
    }

    @Test
    void createNews_persistsAndMapsNewsId() {
        CreateNewsRequest request = CreateNewsRequest.builder()
                .title("Tin mới")
                .content("<p>Nội dung</p>")
                .category("Tech")
                .status("DRAFT")
                .build();

        when(newsRepository.save(any(News.class))).thenAnswer(inv -> {
            News n = inv.getArgument(0);
            n.setId(NEWS_ID);
            return n;
        });

        NewsResponse response = newsService.createNews(request, AUTHOR_ID, "Author");

        assertEquals(NEWS_ID, response.getNewsID());
        assertEquals("Tin mới", response.getTitle());
        assertEquals("DRAFT", response.getStatus());
        verify(newsRepository).save(any(News.class));
    }

    @Test
    void updateNews_updatesNonNullFieldsAndReplacesImages() {
        News news = sampleNews(NewsStatus.DRAFT);
        NewsImage oldImage = NewsImage.builder().id(1L).url("http://old").priority(1).news(news).build();
        news.getImages().add(oldImage);

        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsRepository.save(any(News.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateNewsRequest request = UpdateNewsRequest.builder()
                .title("Tiêu đề mới")
                .images(List.of(NewsImageRequest.builder().url("http://new").priority(2).build()))
                .build();

        NewsResponse response = newsService.updateNews(NEWS_ID, request);

        assertEquals("Tiêu đề mới", response.getTitle());
        assertEquals("Tech", response.getCategory());
        assertEquals(1, news.getImages().size());
        assertEquals("http://new", news.getImages().get(0).getUrl());
    }

    @Test
    void deleteNews_deletesCloudinaryAndEntity() {
        News news = sampleNews(NewsStatus.DRAFT);
        news.getImages().add(NewsImage.builder().id(1L).url("http://img").news(news).build());
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));

        newsService.deleteNews(NEWS_ID);

        verify(imageService).deleteImage("http://img");
        verify(newsRepository).delete(news);
    }

    @Test
    void publishNews_setsPublishedStatus() {
        News news = sampleNews(NewsStatus.DRAFT);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsRepository.save(news)).thenReturn(news);

        NewsResponse response = newsService.publishNews(NEWS_ID);

        assertEquals("PUBLISHED", response.getStatus());
        assertEquals(NewsStatus.PUBLISHED, news.getStatus());
    }

    @Test
    void archiveNews_setsArchivedStatus() {
        News news = sampleNews(NewsStatus.PUBLISHED);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsRepository.save(news)).thenReturn(news);

        NewsResponse response = newsService.archiveNews(NEWS_ID);

        assertEquals("ARCHIVED", response.getStatus());
    }

    @Test
    void restoreNews_setsDraftFromArchived() {
        News news = sampleNews(NewsStatus.ARCHIVED);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsRepository.save(news)).thenReturn(news);

        NewsResponse response = newsService.restoreNews(NEWS_ID);

        assertEquals("DRAFT", response.getStatus());
    }

    @Test
    void restoreNews_nonArchived_throws() {
        News news = sampleNews(NewsStatus.PUBLISHED);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));

        assertThrows(BadRequestException.class, () -> newsService.restoreNews(NEWS_ID));
    }

    @Test
    void getPublishedNewsById_onlyPublished() {
        News news = sampleNews(NewsStatus.PUBLISHED);
        news.setViews(5L);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsRepository.save(news)).thenAnswer(inv -> inv.getArgument(0));

        NewsResponse response = newsService.getPublishedNewsById(NEWS_ID);

        assertEquals(6L, response.getViews());
    }

    @Test
    void getPublishedNewsById_draft_throwsNotFound() {
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(sampleNews(NewsStatus.DRAFT)));

        assertThrows(NewsNotFoundException.class, () -> newsService.getPublishedNewsById(NEWS_ID));
    }

    @Test
    void getNewsById_incrementsViews() {
        News news = sampleNews(NewsStatus.PUBLISHED);
        news.setViews(5L);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsRepository.save(news)).thenAnswer(inv -> inv.getArgument(0));

        NewsResponse response = newsService.getNewsById(NEWS_ID);

        assertEquals(6L, response.getViews());
        assertEquals(6L, news.getViews());
    }

    @Test
    void getNewsByStatusAndFeatured_callsCorrectRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        News news = sampleNews(NewsStatus.PUBLISHED);
        Page<News> page = new PageImpl<>(List.of(news));
        when(newsRepository.findByStatusAndFeaturedOrderByCreatedAtDesc(
                NewsStatus.PUBLISHED, true, pageable)).thenReturn(page);

        Page<NewsResponse> result = newsService.getNewsByStatusAndFeatured(
                NewsStatus.PUBLISHED, true, pageable);

        assertEquals(1, result.getTotalElements());
        verify(newsRepository).findByStatusAndFeaturedOrderByCreatedAtDesc(
                NewsStatus.PUBLISHED, true, pageable);
    }

    @Test
    void searchNewsByTitle_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        News news = sampleNews(NewsStatus.DRAFT);
        when(newsRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc("tin", pageable))
                .thenReturn(new PageImpl<>(List.of(news)));

        Page<NewsResponse> result = newsService.searchNewsByTitle("tin", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(NEWS_ID, result.getContent().get(0).getNewsID());
    }

    @Test
    void uploadNewsImages_persistsWithIncreasingPriority() {
        News news = sampleNews(NewsStatus.DRAFT);
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
        when(newsImageRepository.findByNewsIdOrderByPriorityAsc(NEWS_ID)).thenReturn(List.of(
                NewsImage.builder().id(1L).priority(3).news(news).url("http://existing").build()
        ));
        MultipartFile file = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1});
        when(imageService.uploadMultipleImages(any(), eq("bookstore/news")))
                .thenReturn(List.of(
                        Map.of("url", "http://cloud/1.jpg"),
                        Map.of("url", "http://cloud/2.jpg")
                ));
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));

        NewsResponse response = newsService.uploadNewsImages(NEWS_ID, List.of(file));

        assertNotNull(response);
        ArgumentCaptor<NewsImage> captor = ArgumentCaptor.forClass(NewsImage.class);
        verify(newsImageRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<NewsImage> saved = captor.getAllValues();
        assertEquals(4, saved.get(0).getPriority());
        assertEquals(5, saved.get(1).getPriority());
    }

    @Test
    void deleteNewsImage_newsNotFound_throws() {
        when(newsRepository.existsById(NEWS_ID)).thenReturn(false);

        assertThrows(NewsNotFoundException.class, () ->
                newsService.deleteNewsImage(NEWS_ID, 99L));
    }

    @Test
    void deleteNewsImage_imageNotBelongingToNews_throwsForbidden() {
        News news = sampleNews(NewsStatus.DRAFT);
        News otherNews = sampleNews(NewsStatus.DRAFT);
        otherNews.setId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        NewsImage image = NewsImage.builder().id(5L).url("http://img").news(otherNews).build();

        when(newsRepository.existsById(NEWS_ID)).thenReturn(true);
        when(newsImageRepository.findById(5L)).thenReturn(Optional.of(image));

        assertThrows(ForbiddenException.class, () ->
                newsService.deleteNewsImage(NEWS_ID, 5L));
    }

    @Test
    void getNewsStatistics_mapsAggregates() {
        when(newsRepository.count()).thenReturn(10L);
        when(newsRepository.countByStatus(NewsStatus.PUBLISHED)).thenReturn(6L);
        when(newsRepository.countByStatus(NewsStatus.DRAFT)).thenReturn(3L);
        when(newsRepository.countByStatus(NewsStatus.ARCHIVED)).thenReturn(1L);
        when(newsRepository.countByFeaturedTrue()).thenReturn(2L);
        when(newsRepository.countByCreatedAtBetween(any(), any())).thenReturn(1L, 2L, 3L, 4L);
        when(newsRepository.sumAllViews()).thenReturn(100L);
        when(newsRepository.countByCategoryGroup()).thenReturn(
                List.<Object[]>of(new Object[]{"Tech", 5L}));
        when(newsRepository.findAllByOrderByViewsDesc(any())).thenReturn(
                new PageImpl<>(List.of(sampleNews(NewsStatus.PUBLISHED))));
        when(newsRepository.getViewsTrendBetween(any(), any())).thenReturn(new ArrayList<>());
        when(newsRepository.sumViewsBetween(any(), any())).thenReturn(50L, 40L);

        NewsStatsResponse stats = newsService.getNewsStatistics();

        assertEquals(10L, stats.getTotalNews());
        assertEquals(6L, stats.getPublishedNews());
        assertEquals(3L, stats.getDraftNews());
        assertEquals(1L, stats.getArchivedNews());
        assertEquals(2L, stats.getFeaturedNews());
        assertEquals(100L, stats.getTotalViews());
        assertEquals(1, stats.getNewsByCategory().size());
        assertEquals("Tech", stats.getNewsByCategory().get(0).getCategory());
        assertEquals(1, stats.getTopViewedNews().size());
    }

    private News sampleNews(NewsStatus status) {
        return News.builder()
                .id(NEWS_ID)
                .title("Tin test")
                .content("<p>content</p>")
                .summary("summary")
                .category("Tech")
                .tags(new ArrayList<>(List.of("java")))
                .views(0L)
                .featured(false)
                .status(status)
                .authorId(AUTHOR_ID)
                .authorName("Author")
                .metadata("{}")
                .images(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
