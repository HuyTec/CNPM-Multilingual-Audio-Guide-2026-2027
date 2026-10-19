package com.application.dto;

import java.util.List;

public record LocationSearchResponse(
        List<LocationResponse> items, int page, int size, long totalElements,
        long totalPages, String message, List<CategoryResponse> suggestedCategories) {
}
