package com.application.repository;
import com.application.dto.AdminAuthDto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
@Repository @Profile("demo")
public class AdminAuthRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AdminAuthRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void seed(String username,String hash,String role) {
        jdbc.update("INSERT INTO admin_accounts(username,password_hash,role) VALUES(:u,:h,:r) ON CONFLICT DO NOTHING",Map.of("u",username,"h",hash,"r",role));
    }
    public Optional<Account> account(String username) {
        return jdbc.query("SELECT username,password_hash,role FROM admin_accounts WHERE username=:u AND active",Map.of("u",username),(r,n)->new Account(r.getString(1),r.getString(2),r.getString(3))).stream().findFirst();
    }
    public void session(String hash,String username,Instant expiry) {
        jdbc.update("INSERT INTO admin_sessions(token_hash,username,expires_at) VALUES(:h,:u,:e)",Map.of("h",hash,"u",username,"e",Timestamp.from(expiry)));
    }
    public Optional<Identity> identity(String hash) {
        return jdbc.query("SELECT a.username,a.role FROM admin_sessions s JOIN admin_accounts a ON a.username=s.username WHERE s.token_hash=:h AND s.expires_at>CURRENT_TIMESTAMP AND a.active",Map.of("h",hash),(r,n)->new Identity(r.getString(1),r.getString(2))).stream().findFirst();
    }
    public void logout(String hash) { jdbc.update("DELETE FROM admin_sessions WHERE token_hash=:h",Map.of("h",hash)); }
}
