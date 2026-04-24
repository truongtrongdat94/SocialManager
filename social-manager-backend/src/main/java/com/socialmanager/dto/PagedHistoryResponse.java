package com.socialmanager.dto;

import java.util.List;

public record PagedHistoryResponse<T>(
        List<T> items,
        long total,
        int page,
        int size,
        boolean hasNext
) {
}