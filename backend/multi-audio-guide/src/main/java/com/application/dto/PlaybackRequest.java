package com.application.dto;

import java.util.UUID;

public record PlaybackRequest(UUID eventId, UUID contentId) { }
