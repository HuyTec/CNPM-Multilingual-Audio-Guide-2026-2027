package com.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FeedbackDto {
    private FeedbackDto() { }
    public record Item(UUID id, UUID locationId, String location, String category, String content, Integer rating,
                       String status, String created, Instant updated, Instant createdAt) { }
    public record Page(List<Item> items, int page, int size, long total, long totalPages, List<String> categories) { }
    public record Query(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        String category, String status, Integer page, Integer size) { }
    public record StatusRequest(@NotBlank @Pattern(regexp = "NEW|IN_REVIEW|RESOLVED") String status) { }
    public record Criteria(Instant start, Instant end, String category, String status, int page, int size) { }
}
