package com.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsResponse(LocalDate from, LocalDate to, String timeZone, Instant generatedAt,
                                long totalPlaybacks, long onlinePlaybacks, long offlinePlaybacks,
                                List<CountItem> topLocations, List<CountItem> languages) {
    public record CountItem(String label, long count) { }
}
