package com.application.mapper;

import com.application.dto.CategoryResponse;
import com.application.dto.LocationResponse;
import com.application.model.Category;
import com.application.model.Location;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryMapper {
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.id(), category.name(), category.locationCount());
    }

    public LocationResponse toResponse(Location location) {
        return new LocationResponse(location.id(), location.name(), location.categoryId(),
                location.categoryName(), location.latitude(), location.longitude(), location.geofenceRadiusM(), location.names(), location.imageUrl());
    }
}
