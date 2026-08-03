package com.rzodeczko.application.dto;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}
