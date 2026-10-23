package com.application.dto;

import java.util.UUID;
import java.time.Instant;

public record PlaybackRequest(UUID eventId, UUID contentId, Instant occurredAt, Boolean offline) {
    public PlaybackRequest(UUID eventId, UUID contentId) { this(eventId, contentId, null, null); }
}
