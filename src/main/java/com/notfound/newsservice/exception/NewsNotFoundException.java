package com.notfound.newsservice.exception;

import java.util.UUID;

public class NewsNotFoundException extends RuntimeException {

    public NewsNotFoundException(UUID id) {
        super("Không tìm thấy tin tức với id: " + id);
    }

    public NewsNotFoundException(String message) {
        super(message);
    }
}
