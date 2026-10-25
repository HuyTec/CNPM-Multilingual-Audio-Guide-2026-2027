package com.application.repository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;
@Repository @Profile("demo")
public class SigningKeyRepository {
    public record Keys(byte[] privateKey,byte[] publicKey) { }
    private final NamedParameterJdbcTemplate jdbc;
    public SigningKeyRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Keys loadOrCreate(Keys candidate) {
        jdbc.update("INSERT INTO package_signing_keys(singleton,private_key,public_key) VALUES(TRUE,:p,:u) ON CONFLICT DO NOTHING",Map.of("p",candidate.privateKey(),"u",candidate.publicKey()));
        return jdbc.queryForObject("SELECT private_key,public_key FROM package_signing_keys WHERE singleton",Map.of(),(r,n)->new Keys(r.getBytes(1),r.getBytes(2)));
    }
}
