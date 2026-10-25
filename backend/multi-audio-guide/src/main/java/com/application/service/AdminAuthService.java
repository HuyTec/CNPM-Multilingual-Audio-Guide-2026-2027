package com.application.service;
import com.application.dto.AdminAuthDto.*;
import com.application.repository.AdminAuthRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
@Service @Profile("demo")
public class AdminAuthService {
    private final AdminAuthRepository repository;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
    public AdminAuthService(AdminAuthRepository repository) { this.repository=repository; }
    public void seed(String username,String password,String role) { if(!password.isBlank()) repository.seed(username,encoder.encode(password),role); }
    public Session login(Login request) {
        var account=repository.account(request.username()).orElseThrow(AdminAuthService::unauthorized);
        if(!encoder.matches(request.password(),account.passwordHash())) throw unauthorized();
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry=Instant.now().plusSeconds(8*3600);
        repository.session(hash(token),account.username(),expiry);
        return new Session(token,account.username(),account.role(),expiry);
    }
    public Optional<Identity> identity(String token) { return token==null?Optional.empty():repository.identity(hash(token)); }
    public void logout(String token) { repository.logout(hash(token)); }
    private static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Tên đăng nhập hoặc mật khẩu không đúng."); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception error) { throw new IllegalStateException(error); }
    }
}
