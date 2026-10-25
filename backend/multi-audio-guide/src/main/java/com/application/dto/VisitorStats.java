package com.application.dto;

import java.time.Instant;
import java.util.List;

public record VisitorStats(Instant generatedAt, int activeWindowMinutes, List<PlatformStats> platforms) {
    public record PlatformStats(String platform, long totalInstallations, long activeInstallations) { }
}
