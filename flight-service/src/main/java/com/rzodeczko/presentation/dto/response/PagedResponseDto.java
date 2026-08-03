package com.rzodeczko.presentation.dto.response;

import java.util.List;

public record PagedResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {}
