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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPublished_returnsApiResponse() throws Exception {
        Page<NewsResponse> page = new PageImpl<>(List.of(), Pageable.unpaged(), 0);
        Mockito.when(newsService.getPublishedNews(any())).thenReturn(page);

        mockMvc.perform(get("/api/news/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
