package com.application.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository @Profile("demo")
public class LocationImageRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public LocationImageRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void insert(UUID id, UUID location, byte[] bytes, String actor) {
        jdbc.update("INSERT INTO location_image_uploads(id,location_id,data,uploaded_by) VALUES(:id,:l,:b,:a)",
                Map.of("id",id,"l",location,"b",bytes,"a",actor));
    }
    public boolean belongs(UUID location, String url) {
        return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM location_image_uploads WHERE location_id=:l AND '/api/content-images/'||id::text||'.png'=:u)", Map.of("l",location,"u",url),Boolean.class);
    }
    public Optional<byte[]> read(UUID id, boolean preview) {
        return jdbc.query("""
                SELECT u.data FROM location_image_uploads u WHERE u.id=:id AND
                (:preview OR EXISTS(SELECT 1 FROM locations l WHERE l.id=u.location_id AND l.status='APPROVED'
                    AND l.image_url='/api/content-images/'||u.id::text||'.png'))
                """,Map.of("id",id,"preview",preview),(r,n)->r.getBytes(1)).stream().findFirst();
    }
}
