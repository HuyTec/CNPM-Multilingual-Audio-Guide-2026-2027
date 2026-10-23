package com.application.service;

import com.application.model.GuideContent;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared Web/Android demo publication. One consistent DB snapshot, signed exact UTF-8 bytes. */
@Service
@Profile("demo")
public class ContentPackageService {
    public record Asset(String url, long sizeBytes, String sha256) { }
    public record Manifest(int schemaVersion, String scope, String version, String status, boolean isBase,
                           long sizeBytes, String sha256, String signature, String keyId,
                           String downloadUrl, List<Asset> assets) { }
    public record Bundle(Manifest manifest, byte[] bytes) { }
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final KeyPair key;

    public ContentPackageService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); key = generator.generateKeyPair(); }
        catch (Exception exception) { throw new IllegalStateException("Cannot initialize demo package signing", exception); }
    }

    public Map<String, String> publicKey() {
        String encoded = Base64.getEncoder().encodeToString(key.getPublic().getEncoded());
        return Map.of("algorithm", "RSASSA-PKCS1-v1_5/SHA-256", "keyId", sha(key.getPublic().getEncoded()), "spki", encoded);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Bundle build(String scope) {
        if (!List.of("BASE", "OPTIONAL").contains(scope)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại gói không hợp lệ.");
        boolean base = scope.equals("BASE");
        String category = "10000000-0000-0000-0000-000000000101";
        String filter = base ? "l.category_id=?::uuid" : "l.category_id<>?::uuid";
        List<Map<String, Object>> locations = jdbc.query("""
                SELECT l.id,l.name,l.category_id,c.name AS category_name,
                       ST_Y(l.coordinates::geometry) AS lat,ST_X(l.coordinates::geometry) AS lng,l.geofence_radius_m
                FROM locations l JOIN categories c ON c.id=l.category_id
                WHERE l.status='APPROVED' AND c.is_active AND
                """ + filter + " ORDER BY l.id", (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("locationId", rs.getString("id")); item.put("name", rs.getString("name"));
            item.put("categoryId", rs.getString("category_id")); item.put("categoryName", rs.getString("category_name"));
            item.put("latitude", rs.getDouble("lat")); item.put("longitude", rs.getDouble("lng"));
            item.put("geofenceRadiusM", rs.getInt("geofence_radius_m")); return item;
        }, category);
        List<GuideContent> guides = jdbc.query("""
                SELECT DISTINCT ON (g.location_id,g.language_code,g.script_type)
                    g.id,g.location_id,l.name,g.language_code,g.script_type,g.script_text,a.storage_key
                FROM guide_content_revisions g JOIN locations l ON l.id=g.location_id
                JOIN categories c ON c.id=l.category_id JOIN languages lang ON lang.code=g.language_code
                LEFT JOIN audio_assets a ON a.content_id=g.id
                WHERE l.status='APPROVED' AND g.status='APPROVED' AND c.is_active AND lang.is_active
                  AND g.script_text ~ '[^[:space:]]' AND
                """ + filter + " ORDER BY g.location_id,g.language_code,g.script_type,g.revision_no DESC,g.id",
                (rs, row) -> new GuideContent(rs.getObject("id", UUID.class), rs.getObject("location_id", UUID.class), rs.getString("name"),
                        rs.getString("language_code"),rs.getString("script_type"),rs.getString("script_text"),rs.getString("storage_key")), category);
        if (locations.isEmpty() || guides.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có gói đã duyệt.");
        List<Asset> assets = guides.stream().map(GuideContent::audioUrl).filter(url -> url != null).distinct().sorted().map(this::asset).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1); payload.put("status", "APPROVED"); payload.put("scope", scope); payload.put("isBase", base);
        payload.put("locations", locations); payload.put("guides", guides);
        payload.put("languages", guides.stream().map(GuideContent::languageCode).distinct().sorted().toList()); payload.put("assets", assets);
        var geofencing = geofencing(locations);
        if (!geofencing.isEmpty()) payload.put("geofencing", geofencing);
        byte[] bytes = mapper.writeValueAsBytes(payload);
        String checksum = sha(bytes); String version = scope.toLowerCase() + "-" + checksum;
        try {
            Signature signer = Signature.getInstance("SHA256withRSA"); signer.initSign(key.getPrivate()); signer.update(bytes);
            Manifest manifest = new Manifest(1, scope, version, "APPROVED", base, bytes.length, checksum,
                    Base64.getEncoder().encodeToString(signer.sign()), sha(key.getPublic().getEncoded()),
                    "/api/content-packages/" + scope + "/" + version, assets);
            return new Bundle(manifest, bytes);
        } catch (Exception exception) { throw new IllegalStateException("Cannot sign content package", exception); }
    }

    private Map<String, Object> geofencing(List<Map<String, Object>> locations) {
        var cooldown = jdbc.queryForList("SELECT cooldown_seconds FROM geofence_configuration WHERE singleton", Integer.class);
        if (cooldown.isEmpty()) return Map.of();
        Map<String, Object> names = new LinkedHashMap<>();
        java.time.Instant latest = java.time.Instant.EPOCH;
        for (var location : locations) {
            var surveys = jdbc.query("""
                    SELECT s.surveyed_at,s.localized_names::text AS names
                    FROM location_geofence_surveys s
                    WHERE s.location_id=?::uuid AND s.latitude=? AND s.longitude=? AND s.radius_m=?
                      AND s.surveyed_at <= CURRENT_TIMESTAMP
                    """, (rs, row) -> Map.of("at", rs.getTimestamp("surveyed_at").toInstant(), "names", rs.getString("names")),
                    location.get("locationId"), location.get("latitude"), location.get("longitude"), location.get("geofenceRadiusM"));
            if (surveys.isEmpty()) continue;
            var survey = surveys.getFirst();
            var at = (java.time.Instant) survey.get("at");
            if (at.isAfter(latest)) latest = at;
            names.put((String) location.get("locationId"), mapper.readTree((String) survey.get("names")));
        }
        return names.isEmpty() ? Map.of() : Map.of("cooldownSeconds", cooldown.getFirst(), "surveyedAt", latest.toString(), "localizedNames", names);
    }

    private Asset asset(String url) {
        try {
            byte[] bytes;
            if (url.matches("/demo-audio/[a-z-]+\\.wav")) bytes=new ClassPathResource("static"+url).getContentAsByteArray();
            else if(url.matches("/api/content-audio/[0-9a-f-]{36}\\.(mp3|wav|ogg)")) bytes=jdbc.queryForObject("SELECT data FROM admin_audio_uploads WHERE '/api/content-audio/'||id::text||'.'||format=?",byte[].class,url);
            else throw new IllegalArgumentException("Unsupported audio source");
            if(bytes==null)throw new IllegalStateException("Missing audio");
            return new Asset(url, bytes.length, sha(bytes));
        }
        catch (Exception exception) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể chuẩn bị audio cho gói."); }
    }
    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
