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
    public record Location(UUID locationId, String name, UUID categoryId, String categoryName,
                           double latitude, double longitude, int geofenceRadiusM) { }
    public record Geofencing(int cooldownSeconds, Instant surveyedAt, Map<String, Map<String, String>> localizedNames) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"schemaVersion","status","scope","isBase","locations","guides","languages","assets","geofencing"})
    public record Payload(int schemaVersion, String status, String scope, boolean isBase, List<Location> locations,
                          List<GuideContent> guides, List<String> languages,
                          List<com.application.service.ContentPackageService.Asset> assets, Geofencing geofencing) { }
}
