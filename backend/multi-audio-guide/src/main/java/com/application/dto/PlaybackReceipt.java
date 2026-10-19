package com.application.dto;

import java.util.UUID;

public record PlaybackReceipt(UUID eventId, boolean created) { }
