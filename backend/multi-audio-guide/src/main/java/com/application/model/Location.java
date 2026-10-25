package com.application.model;

import java.util.UUID;

public record Location(
        UUID id, String name, UUID categoryId, String categoryName,
        Double latitude, Double longitude, Integer geofenceRadiusM, java.util.Map<String,String> names, String imageUrl) {
    public Location(UUID id,String name,UUID categoryId,String categoryName,Double latitude,Double longitude,Integer geofenceRadiusM,java.util.Map<String,String> names) {
        this(id,name,categoryId,categoryName,latitude,longitude,geofenceRadiusM,names,null);
    }
    public Location(UUID id,String name,UUID categoryId,String categoryName,Double latitude,Double longitude,Integer geofenceRadiusM) {
        this(id,name,categoryId,categoryName,latitude,longitude,geofenceRadiusM,java.util.Map.of());
    }
}
