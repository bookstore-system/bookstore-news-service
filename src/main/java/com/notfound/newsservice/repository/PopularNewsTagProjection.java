package com.notfound.newsservice.repository;

public interface PopularNewsTagProjection {

    String getTag();

    String getNormalizedTag();

    Long getSearchCount();
}
