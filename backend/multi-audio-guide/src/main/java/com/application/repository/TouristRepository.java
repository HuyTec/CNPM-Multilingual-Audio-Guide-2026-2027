package com.application.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class TouristRepository {
    private final JdbcTemplate jdbc;
    public TouristRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public UUID guest(String keyHash) {
        var found = jdbc.query("SELECT guest_id FROM tourist_installations WHERE key_hash=?", (r,n) -> r.getObject(1, UUID.class), keyHash);
        if (!found.isEmpty()) return found.getFirst();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tourist_users(id) VALUES (?)", id);
        jdbc.update("INSERT INTO tourist_installations(key_hash,guest_id) VALUES (?,?) ON CONFLICT DO NOTHING", keyHash,id);
        UUID actual = jdbc.queryForObject("SELECT guest_id FROM tourist_installations WHERE key_hash=?", UUID.class,keyHash);
        if (!id.equals(actual)) jdbc.update("DELETE FROM tourist_users WHERE id=?",id);
        return actual;
    }
    public Map<String,Object> challenge(UUID id) {
        var rows = jdbc.queryForList("SELECT * FROM tourist_otp WHERE id=? FOR UPDATE",id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    /** Database advisory lock serializes rate limits across concurrent requests/instances. */
    public void lock(String value) { jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",value); }
    public boolean limited(String email,String installation,String ip) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT count(*) FILTER (WHERE email=?) >= 5
                OR count(*) FILTER (WHERE installation_hash=?) >= 10
                OR count(*) FILTER (WHERE ip_hash=?) >= 30
                OR count(*) FILTER (WHERE email=? AND created_at > CURRENT_TIMESTAMP-INTERVAL '60 seconds') > 0
            FROM tourist_otp WHERE created_at > CURRENT_TIMESTAMP-INTERVAL '1 hour'
            """,Boolean.class,email,installation,ip,email));
    }
    public void createChallenge(UUID id,String email,String installation,String ip,String hash,String salt) {
        jdbc.update("UPDATE tourist_otp SET consumed=TRUE WHERE email=? AND installation_hash=?",email,installation);
        jdbc.update("INSERT INTO tourist_otp(id,email,installation_hash,ip_hash,code_hash,salt,expires_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP+INTERVAL '5 minutes')",
            id,email,installation,ip,hash,salt);
    }
    public void attempted(UUID id,boolean success) { jdbc.update("UPDATE tourist_otp SET attempts=attempts+1, consumed=? WHERE id=?",success,id); }
    public UUID emailUser(String email) {
        jdbc.update("INSERT INTO tourist_users(id,email) VALUES (?,?) ON CONFLICT(email) DO NOTHING",UUID.randomUUID(),email);
        return jdbc.queryForObject("SELECT id FROM tourist_users WHERE email=?",UUID.class,email);
    }
    public void merge(UUID guest,UUID user) { jdbc.update("UPDATE tourist_users SET merged_into=? WHERE id=? AND email IS NULL AND (merged_into IS NULL OR merged_into=?)",user,guest,user); }
    public void session(String tokenHash,UUID user,Instant expires) {
        jdbc.update("INSERT INTO tourist_sessions(token_hash,user_id,expires_at) VALUES (?,?,?)",tokenHash,user,Timestamp.from(expires));
    }
    public Optional<UUID> user(String tokenHash) { return jdbc.query("SELECT user_id FROM tourist_sessions WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP",(r,n)->r.getObject(1,UUID.class),tokenHash).stream().findFirst(); }
    public void revoke(String tokenHash) { jdbc.update("DELETE FROM tourist_sessions WHERE token_hash=?",tokenHash); }
}
