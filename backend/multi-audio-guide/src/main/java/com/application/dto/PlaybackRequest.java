package com.application.dto;

import java.util.UUID;
import java.time.Instant;

public record PlaybackRequest(UUID eventId, UUID contentId, Instant occurredAt, Boolean offline,
                              UUID installationId, VisitorHeartbeat.Platform platform) {
    public PlaybackRequest(UUID eventId, UUID contentId) { this(eventId, contentId, null, null, null, null); }
    public PlaybackRequest(UUID eventId, UUID contentId, Instant occurredAt, Boolean offline) {
        this(eventId, contentId, occurredAt, offline, null, null);
    }
}
