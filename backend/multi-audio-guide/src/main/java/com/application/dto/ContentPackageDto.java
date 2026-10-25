package com.application.dto;

import com.application.model.GuideContent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ContentPackageDto {
    private ContentPackageDto() { }
    public record PublicKey(String algorithm, String keyId, String spki) { }
    public record Asset(String url, long sizeBytes, String sha256) { }
    public record Manifest(int schemaVersion, String scope, String version, String status, boolean isBase,
                           long sizeBytes, String sha256, String signature, String keyId, String downloadUrl, List<Asset> assets) { }
    public record Bundle(Manifest manifest, byte[] bytes) { }
    public record Location(UUID locationId, String name, UUID categoryId, String categoryName,
                           double latitude, double longitude, int geofenceRadiusM, Map<String,String> names, String imageUrl) {
        public Location(UUID id,String name,UUID category,String categoryName,double latitude,double longitude,int radius,Map<String,String> names) { this(id,name,category,categoryName,latitude,longitude,radius,names,null); }
        public Location(UUID id,String name,UUID category,String categoryName,double latitude,double longitude,int radius) { this(id,name,category,categoryName,latitude,longitude,radius,Map.of()); }
    }
    public record Geofencing(int cooldownSeconds, Instant surveyedAt, Map<String, Map<String, String>> localizedNames) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"schemaVersion","status","scope","isBase","locations","guides","languages","assets","geofencing"})
    public record Payload(int schemaVersion, String status, String scope, boolean isBase, List<Location> locations,
                          List<GuideContent> guides, List<String> languages,
                          List<Asset> assets, Geofencing geofencing) { }
}
