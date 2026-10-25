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
    public enum Origin { MANUAL, AUTO_TRANSLATED, EDITED_AFTER_TRANSLATE }
    public record Language(String code, String label) { }
    public record Config(List<String> categories, List<Language> languages) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asset(UUID locationId, String languageCode, String scriptType, String packageVersion,
                        @Size(max = 100000) String text, String audioUrl, String audioName, String audioStatus) { }
    public record Published(String version, String name, Map<String, Asset> assets, Map<String,String> names,
                            String category,String latitude,String longitude,String radius) {
        public Published(String version,String name,Map<String,Asset> assets) { this(version,name,assets,Map.of(),null,null,null,null); }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SaveRequest(@NotNull UUID id, @Size(max = 255) String name,
                              @Size(max = 255) String category, @Size(max = 255) String latitude,
                              @Size(max = 255) String longitude, @Size(max = 255) String radius,
                              @NotNull String status, @NotNull @Size(max = 100) Map<String, @Valid Asset> assets,
                              Integer editVersion, @Size(max = 20) Map<String, @Size(max = 255) String> names, Map<String, Origin> fieldOrigins) {
        public SaveRequest(UUID id,String name,String category,String latitude,String longitude,String radius,String status,Map<String,Asset> assets,Integer editVersion,Map<String,String> names) { this(id,name,category,latitude,longitude,radius,status,assets,editVersion,names,null); }
        public SaveRequest(UUID id, String name, String category, String latitude, String longitude, String radius,
                           String status, Map<String, Asset> assets, Integer editVersion) {
            this(id, name, category, latitude, longitude, radius, status, assets, editVersion, null);
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDetail(UUID id, String name, String category, String latitude, String longitude,
                                 String radius, String status, String packageVersion, Map<String, Asset> assets,
                                 int editVersion, String updatedBy, Instant updatedAt, Published published,
                                 Map<String, String> names, String reviewerComment, Map<String, Origin> fieldOrigins) {
        public LocationDetail(UUID id,String name,String category,String latitude,String longitude,String radius,String status,String packageVersion,Map<String,Asset> assets,int editVersion,String updatedBy,Instant updatedAt,Published published,Map<String,String> names,String reviewerComment) { this(id,name,category,latitude,longitude,radius,status,packageVersion,assets,editVersion,updatedBy,updatedAt,published,names,reviewerComment,Map.of()); }
        public LocationDetail(UUID id, String name, String category, String latitude, String longitude, String radius,
                              String status, String packageVersion, Map<String, Asset> assets, int editVersion,
                              String updatedBy, Instant updatedAt, Published published) {
            this(id, name, category, latitude, longitude, radius, status, packageVersion, assets, editVersion,
                    updatedBy, updatedAt, published, Map.of("vi", name == null ? "" : name), null);
        }
    }
    public record UploadResponse(String audioUrl, String audioName, String audioStatus) { }
    public record Audio(byte[] bytes, String format) { }
    public record Workspace(LocationDetail original, LocationDetail draft) { }
}
