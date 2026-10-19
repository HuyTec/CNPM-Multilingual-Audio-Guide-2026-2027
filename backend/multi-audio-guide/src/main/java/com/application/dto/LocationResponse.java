package com.application.dto;

import java.util.UUID;

public record LocationResponse(
        UUID locationId, String name, UUID categoryId, String categoryName,
        Double latitude, Double longitude, Integer geofenceRadiusM) {
}
