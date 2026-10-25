package com.application.repository;

import com.application.dto.AdminContentDto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** SQL, JDBC row mapping and JSONB storage for the editor workspace. */
@Repository
@Profile("demo")
public class AdminContentRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    public AdminContentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Config config() {
        return new Config(jdbc.queryForList("SELECT name FROM categories WHERE is_active ORDER BY name", Map.of(), String.class),
                jdbc.query("SELECT code,display_name FROM languages WHERE is_active ORDER BY code", Map.of(),
                        (rs, row) -> new Language(rs.getString(1).toLowerCase(Locale.ROOT), rs.getString(2))));
    }
    /** Two SELECTs for a nonempty catalog, independent of its number of locations. */
    public List<Workspace> workspaces(UUID id) {
        var params = Map.of("all", id == null, "id", id == null ? new UUID(0, 0) : id);
        record Row(UUID id, LocationDetail original, LocationDetail draft) { }
        List<Row> rows = jdbc.query("""
                WITH ids AS (SELECT id FROM locations UNION SELECT location_id FROM admin_content_drafts)
                SELECT ids.id,l.id AS original_id,l.name,c.name AS category,l.status,
                    ST_Y(l.coordinates::geometry) AS lat,ST_X(l.coordinates::geometry) AS lng,
                    l.geofence_radius_m AS radius,d.payload::text,d.status AS draft_status,
                    d.edit_version,d.updated_by,d.updated_at,d.reviewer_comment,
                    COALESCE((SELECT jsonb_object_agg(lower(t.language_code),t.name) FROM location_translations t
                        WHERE t.location_id=ids.id),'{}'::jsonb)::text AS names
                FROM ids LEFT JOIN locations l ON l.id=ids.id
                LEFT JOIN categories c ON c.id=l.category_id
                LEFT JOIN admin_content_drafts d ON d.location_id=ids.id
                WHERE :all OR ids.id=:id ORDER BY ids.id
                """, params, (rs, index) -> {
            UUID location = rs.getObject("id", UUID.class);
            LocationDetail original = rs.getObject("original_id") == null ? null : new LocationDetail(location,
                    value(rs.getString("name")), value(rs.getString("category")), value(rs.getString("lat")),
                    value(rs.getString("lng")), value(rs.getString("radius")),
                    "REJECTED".equals(rs.getString("status")) ? "REJECTED" : "APPROVED".equals(rs.getString("status")) ? "APPROVED" : "DRAFT",
                    "Chưa gán phiên bản làm việc", new LinkedHashMap<>(), 0, null, null, null,
                    names(rs.getString("names"), value(rs.getString("name"))), null);
            LocationDetail draft = null;
            if (rs.getString("payload") != null) {
                LocationDetail p = json.readValue(rs.getString("payload"), LocationDetail.class);
                draft = new LocationDetail(location, p.name(), p.category(), p.latitude(), p.longitude(), p.radius(),
                        rs.getString("draft_status"), p.packageVersion(), p.assets(), rs.getInt("edit_version"),
                        rs.getString("updated_by"), rs.getTimestamp("updated_at").toInstant(), null,
                        p.names() == null ? Map.of("vi", value(p.name())) : p.names(), rs.getString("reviewer_comment"));
            }
            return new Row(location, original, draft);
        });
        if (rows.isEmpty()) return List.of();
        Map<UUID, Map<String, Asset>> assets = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT ON (g.location_id,g.language_code,g.script_type)
                    g.location_id,g.language_code,g.script_type,g.script_text,a.storage_key
                FROM guide_content_revisions g JOIN locations l ON l.id=g.location_id
                LEFT JOIN audio_assets a ON a.content_id=g.id
                WHERE g.location_id IN (:ids) AND g.status<>'ARCHIVED'
                  AND (g.status='APPROVED' OR l.status<>'APPROVED')
                ORDER BY g.location_id,g.language_code,g.script_type,g.revision_no DESC,g.id
                """, Map.of("ids", rows.stream().map(Row::id).toList()), rs -> {
            UUID location = rs.getObject(1, UUID.class);
            String lang = rs.getString(2).toLowerCase(Locale.ROOT), type = rs.getString(3), audio = rs.getString(5);
            assets.computeIfAbsent(location, ignored -> new LinkedHashMap<>()).put(lang + ":" + type,
                    new Asset(location, lang, type, "APPROVED", value(rs.getString(4)), audio,
                            audio == null ? null : audio.substring(audio.lastIndexOf('/') + 1), audio == null ? null : "ready"));
        });
        List<Workspace> result = new ArrayList<>();
        for (Row row : rows) {
            LocationDetail p = row.original();
            if (p != null) p.assets().putAll(assets.getOrDefault(row.id(), Map.of()));
            result.add(new Workspace(p, row.draft()));
        }
        return result;
    }
    public boolean categoryActive(String category) {
        return jdbc.queryForObject("SELECT count(*) FROM categories WHERE name=:name AND is_active", Map.of("name", category), Integer.class) > 0;
    }
    public boolean languageActive(String language) {
        return jdbc.queryForObject("SELECT count(*) FROM languages WHERE code=:code AND is_active", Map.of("code", language), Integer.class) > 0;
    }
    public boolean audioBelongs(UUID id, String language, String type, String audio) {
        return jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM admin_audio_uploads WHERE location_id=:id AND language_code=:lang
                  AND script_type=:type AND '/api/content-audio/'||id::text||'.'||format=:audio)
                  OR EXISTS(SELECT 1 FROM audio_assets a JOIN guide_content_revisions g ON g.id=a.content_id
                  WHERE g.location_id=:id AND g.language_code=:lang AND g.script_type=:type AND a.storage_key=:audio)
                """, Map.of("id", id, "lang", language, "type", type, "audio", audio), Boolean.class);
    }
    public boolean save(LocationDetail payload, int expected, String editor) {
        var params = Map.of("id", payload.id(), "payload", json.writeValueAsString(payload), "status", payload.status(),
                "expected", expected, "editor", editor);
        int count = expected == 0 ? jdbc.update("""
                INSERT INTO admin_content_drafts(location_id,payload,status,edit_version,updated_by)
                VALUES (:id,CAST(:payload AS jsonb),:status,1,:editor) ON CONFLICT DO NOTHING
                """, params) : jdbc.update("""
                UPDATE admin_content_drafts SET payload=CAST(:payload AS jsonb),status=:status,
                    edit_version=edit_version+1,updated_by=:editor,updated_at=CURRENT_TIMESTAMP
                WHERE location_id=:id AND edit_version=:expected
                """, params);
        return count == 1;
    }
    public void insertAudio(UUID id, UUID location, String language, String type, String filename, String format, byte[] bytes, String editor) {
        jdbc.update("""
                INSERT INTO admin_audio_uploads(id,location_id,language_code,script_type,filename,format,data,uploaded_by)
                VALUES (:id,:location,:lang,:type,:name,:format,:data,:editor)
                """, Map.of("id", id, "location", location, "lang", language, "type", type, "name", filename,
                "format", format, "data", bytes, "editor", editor));
    }
    public Optional<Audio> audio(UUID id, String format, boolean preview) {
        return jdbc.query("""
                SELECT data,format FROM admin_audio_uploads u WHERE u.id=:id AND u.format=:format AND
                (:preview OR EXISTS(SELECT 1 FROM audio_assets a JOIN guide_content_revisions g ON g.id=a.content_id
                JOIN locations l ON l.id=g.location_id WHERE a.storage_key=:url AND g.status='APPROVED' AND l.status='APPROVED'))
                """, Map.of("id", id, "format", format, "preview", preview, "url", "/api/content-audio/" + id + "." + format),
                (rs, row) -> new Audio(rs.getBytes(1), rs.getString(2))).stream().findFirst();
    }
    private static String value(String value) { return value == null ? "" : value; }
    private Map<String, String> names(String text, String legacy) {
        Map<String, String> result = new LinkedHashMap<>();
        json.readTree(text).properties().forEach(e -> result.put(e.getKey(), e.getValue().asString()));
        if (result.isEmpty() && !legacy.isBlank()) result.put("vi", legacy);
        return result;
    }
}
