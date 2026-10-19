package com.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** Local demo access control only; credentials are configured on the server, never embedded in APK. */
@Component
@Profile("demo")
public class DemoAdminFilter extends OncePerRequestFilter {
    private final String username;
    private final String password;
    public DemoAdminFilter(@Value("${demo.admin.username:admin}") String username,
                           @Value("${demo.admin.password:}") String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (password.isBlank()) { response.sendError(503, "Demo admin password not configured"); return; }
        String supplied = request.getHeader("Authorization");
        String expected = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        if (supplied == null || !MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(401);
            response.setContentType("application/problem+json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"title\":\"Cần xác thực quản trị demo\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
