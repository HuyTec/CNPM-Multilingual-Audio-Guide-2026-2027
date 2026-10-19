package com.application.dto;

import java.util.UUID;

public record CategoryResponse(UUID categoryId, String name, long locationCount) {
}
