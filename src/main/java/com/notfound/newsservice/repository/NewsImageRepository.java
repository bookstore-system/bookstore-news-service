package com.notfound.newsservice.repository;

import com.notfound.newsservice.model.entity.NewsImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NewsImageRepository extends JpaRepository<NewsImage, UUID> {

    List<NewsImage> findByNewsIdOrderByPriorityAsc(UUID newsId);
}
