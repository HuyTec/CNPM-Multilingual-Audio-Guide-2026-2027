package com.application.repository;

import com.application.model.GuideContent;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GuideRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String PUBLIC_GUIDE = """
            SELECT g.id, g.location_id, l.name, g.language_code, g.script_type,
                   g.script_text, a.storage_key
            FROM guide_content_revisions g
            JOIN locations l ON l.id = g.location_id
            JOIN categories c ON c.id = l.category_id
            JOIN languages lang ON lang.code = g.language_code
            LEFT JOIN audio_assets a ON a.content_id = g.id
            WHERE g.status = 'APPROVED' AND l.status = 'APPROVED'
              AND c.is_active AND lang.is_active AND g.script_text ~ '[^[:space:]]'
            """;
    private static final RowMapper<GuideContent> MAPPER = (rs, index) -> new GuideContent(
            rs.getObject("id", UUID.class), rs.getObject("location_id", UUID.class), rs.getString("name"),
            rs.getString("language_code"), rs.getString("script_type"), rs.getString("script_text"), rs.getString("storage_key"));

    public GuideRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Select the latest published revision, preserving script type across language fallback. */
    public Optional<GuideContent> find(UUID locationId, String language, String scriptType) {
        return jdbc.query(PUBLIC_GUIDE + """
                AND g.location_id = :location AND g.script_type = :type
                AND g.language_code IN (:language, 'EN', 'VI')
                ORDER BY CASE g.language_code WHEN :language THEN 0 WHEN 'EN' THEN 1 ELSE 2 END,
                         g.revision_no DESC, g.id LIMIT 1
                """, Map.of("location", locationId, "language", language, "type", scriptType), MAPPER).stream().findFirst();
    }

    public Optional<GuideContent> findPublicById(UUID contentId) {
        return jdbc.query(PUBLIC_GUIDE + " AND g.id = :id", Map.of("id", contentId), MAPPER).stream().findFirst();
    }
}
