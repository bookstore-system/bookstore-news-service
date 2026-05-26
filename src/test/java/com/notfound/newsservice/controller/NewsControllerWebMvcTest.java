package com.notfound.newsservice.controller;

import com.notfound.newsservice.exception.GlobalExceptionHandler;
import com.notfound.newsservice.model.dto.response.NewsResponse;
import com.notfound.newsservice.service.NewsService;
import com.notfound.newsservice.util.UserContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
@Import(GlobalExceptionHandler.class)
class NewsControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NewsService newsService;

    @MockitoBean
    UserContext userContext;

    @Test
    void getAllNews_returnsApiResponse() throws Exception {
        Page<NewsResponse> page = new PageImpl<>(List.of(), Pageable.unpaged(), 0);
        Mockito.when(newsService.getAllNews(any())).thenReturn(page);
        doNothing().when(userContext).requireAdmin();

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPublished_returnsApiResponse() throws Exception {
        Page<NewsResponse> page = new PageImpl<>(List.of(), Pageable.unpaged(), 0);
        Mockito.when(newsService.searchPublishedNews(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/news/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void recordTagSearch_returnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/news/tag-searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tag": "Unity"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPopularTags_returnsApiResponse() throws Exception {
        Mockito.when(newsService.getPopularTags(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/news/popular-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
