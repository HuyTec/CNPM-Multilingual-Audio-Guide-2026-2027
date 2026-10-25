package com.application.dto;

import java.util.UUID;

public record LocationResponse(
        UUID locationId, String name, UUID categoryId, String categoryName,
        Double latitude, Double longitude, Integer geofenceRadiusM, java.util.Map<String,String> names, String imageUrl) {
    public LocationResponse(UUID locationId,String name,UUID categoryId,String categoryName,Double latitude,Double longitude,Integer geofenceRadiusM,java.util.Map<String,String> names) {
        this(locationId,name,categoryId,categoryName,latitude,longitude,geofenceRadiusM,names,null);
    }
    public LocationResponse(UUID locationId,String name,UUID categoryId,String categoryName,Double latitude,Double longitude,Integer geofenceRadiusM) {
        this(locationId,name,categoryId,categoryName,latitude,longitude,geofenceRadiusM,java.util.Map.of());
    }
}
