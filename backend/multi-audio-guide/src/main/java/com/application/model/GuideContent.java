package com.application.model;

import java.util.UUID;

public record GuideContent(UUID contentId, UUID locationId, String locationName,
                           String languageCode, String scriptType, String scriptText, String audioUrl) { }
