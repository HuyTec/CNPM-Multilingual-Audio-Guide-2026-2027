package com.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Typed editor contract; keeps the existing JSON field names and string coordinates. */
public final class AdminContentDto {
    private AdminContentDto() { }
    public record Language(String code, String label) { }
    public record Config(List<String> categories, List<Language> languages) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asset(UUID locationId, String languageCode, String scriptType, String packageVersion,
                        @Size(max = 100000) String text, String audioUrl, String audioName, String audioStatus) { }
    public record Published(String version, String name, Map<String, Asset> assets) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SaveRequest(@NotNull UUID id, @Size(max = 255) String name,
                              @Size(max = 255) String category, @Size(max = 255) String latitude,
                              @Size(max = 255) String longitude, @Size(max = 255) String radius,
                              @NotNull String status, @NotNull @Size(max = 100) Map<String, @Valid Asset> assets,
                              Integer editVersion) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDetail(UUID id, String name, String category, String latitude, String longitude,
                                 String radius, String status, String packageVersion, Map<String, Asset> assets,
                                 int editVersion, String updatedBy, Instant updatedAt, Published published) { }
    public record UploadResponse(String audioUrl, String audioName, String audioStatus) { }
    public record Audio(byte[] bytes, String format) { }
    public record Workspace(LocationDetail original, LocationDetail draft) { }
}
