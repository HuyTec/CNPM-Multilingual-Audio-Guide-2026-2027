package com.application.controller;
import com.application.dto.AdminAuthDto.*;
import com.application.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo") @RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService auth;
    public AdminAuthController(AdminAuthService auth) { this.auth=auth; }
    @PostMapping("/login") public Session login(@Valid @RequestBody Login request) { return auth.login(request); }
    @GetMapping("/me") public Identity me(HttpServletRequest request) { return (Identity)request.getAttribute("adminIdentity"); }
    @PostMapping("/logout") public Identity logout(HttpServletRequest request) { auth.logout(request.getHeader("Authorization").substring(7));return me(request); }
}
