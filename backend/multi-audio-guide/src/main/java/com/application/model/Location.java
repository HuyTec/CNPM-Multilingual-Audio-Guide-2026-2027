package com.application.model;

import java.util.UUID;

public record Location(
        UUID id, String name, UUID categoryId, String categoryName,
        Double latitude, Double longitude, Integer geofenceRadiusM) {
}
