package com.application.service;

import com.application.repository.TouristRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class TouristAuthService {
    public record Challenge(UUID challengeId,int expiresInSeconds,int retryAfterSeconds) { }
    public record Session(UUID userId,String email,String accessToken,Instant expiresAt) { }
    private final TouristRepository users;
    private final TouristMailSender mail;
    private final SecureRandom random=new SecureRandom();
    public TouristAuthService(TouristRepository users,TouristMailSender mail) { this.users=users;this.mail=mail; }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    private String otpHash(String code,String salt) {
        var spec=new PBEKeySpec(code.toCharArray(),HexFormat.of().parseHex(salt),150_000,256);
        try { return HexFormat.of().formatHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    private String installation(String key) {
        if(key==null || !key.matches("[A-Za-z0-9_-]{43}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Định danh thiết bị không hợp lệ.");
        return hash(key);
    }
    @Transactional public UUID guest(String key) { return users.guest(installation(key)); }
    @Transactional public Challenge request(String email,String key,String remoteIp) {
        email=email==null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if(email.length()>254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email không hợp lệ.");
        String device=installation(key), ip=hash(remoteIp);
        // Same lock order prevents deadlocks while enforcing both per-email and per-device limits.
        for(String lock:new TreeSet<>(List.of("email:"+email,"device:"+device,"ip:"+ip))) users.lock(lock);
        if(users.limited(email,device,ip)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng chờ trước khi gửi lại mã.");
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
        byte[] saltBytes=new byte[16]; random.nextBytes(saltBytes); String salt=HexFormat.of().formatHex(saltBytes);
        UUID id=UUID.randomUUID(); users.createChallenge(id,email,device,ip,otpHash(code,salt),salt);
        mail.send(email,code); // Roll back unusable challenges on transport failure. Never return/log the code.
        return new Challenge(id,300,60);
    }
    /** Failed attempts must commit, so invalid codes return a result rather than rolling back the transaction. */
    @Transactional public Optional<Session> verify(UUID id,String code,String key) {
        var row=users.challenge(id);
        if(row==null || !installation(key).equals(row.get("installation_hash")) || Boolean.TRUE.equals(row.get("consumed"))
            || ((Number)row.get("attempts")).intValue()>=5 || ((Timestamp)row.get("expires_at")).toInstant().isBefore(Instant.now())) return Optional.empty();
        boolean valid=code!=null && code.matches("[0-9]{6}") && MessageDigest.isEqual(otpHash(code,(String)row.get("salt")).getBytes(StandardCharsets.US_ASCII),((String)row.get("code_hash")).getBytes(StandardCharsets.US_ASCII));
        users.attempted(id,valid);
        if(!valid) return Optional.empty();
        String email=(String)row.get("email"); UUID user=users.emailUser(email);
        users.merge(users.guest(installation(key)),user);
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires=Instant.now().plus(Duration.ofDays(30));users.session(hash(token),user,expires);
        return Optional.of(new Session(user,email,token,expires));
    }
    @Transactional public UUID listener(String authorization,String key) {
        if(authorization!=null) {
            if(!authorization.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên đăng nhập không hợp lệ.");
            return users.user(hash(authorization.substring(7))).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."));
        }
        return key==null ? null : guest(key); // Existing public Web clients remain compatible, without falsely attributed users.
    }
    public void logout(String authorization) { if(authorization!=null && authorization.startsWith("Bearer ")) users.revoke(hash(authorization.substring(7))); }
}
