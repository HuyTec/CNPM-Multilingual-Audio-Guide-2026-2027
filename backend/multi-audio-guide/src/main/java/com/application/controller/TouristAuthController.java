package com.application.controller;

import com.application.service.TouristAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/tourist")
public class TouristAuthController {
    public record RequestCode(String email) { }
    public record VerifyCode(UUID challengeId,String code) { }
    private final TouristAuthService auth;
    public TouristAuthController(TouristAuthService auth) { this.auth=auth; }
    @PostMapping("/guest") public ResponseEntity<?> guest(@RequestHeader("X-Installation-Key") String key) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("userId",auth.guest(key),"kind","GUEST"));
    }
    @PostMapping("/otp/request") public ResponseEntity<?> request(@RequestBody RequestCode body,@RequestHeader("X-Installation-Key") String key,HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.request(body.email(),key,request.getRemoteAddr()));
    }
    @PostMapping("/otp/verify") public ResponseEntity<?> verify(@RequestBody VerifyCode body,@RequestHeader("X-Installation-Key") String key) {
        if(body.challengeId()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thiếu yêu cầu xác thực.");
        var session=auth.verify(body.challengeId(),body.code(),key).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mã không đúng hoặc đã hết hạn. Vui lòng thử lại hoặc gửi mã mới."));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(session);
    }
    @PostMapping("/logout") public ResponseEntity<?> logout(@RequestHeader(value="Authorization",required=false) String token) {
        auth.logout(token);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
