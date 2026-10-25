package com.application.config;
import com.application.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.Principal;
/** Every administrative request is authorized against a database-backed bearer session. */
@Component @Profile("demo")
public class DemoAdminFilter extends OncePerRequestFilter {
    private final AdminAuthService auth;
    public DemoAdminFilter(AdminAuthService auth) { this.auth=auth; }
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api/admin/") || (path.equals("/api/admin/auth/login") && request.getMethod().equals("POST"));
    }
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        response.setHeader("Cache-Control","no-store");
        String header=request.getHeader("Authorization");
        var identity=auth.identity(header!=null && header.startsWith("Bearer ")?header.substring(7):null);
        if(identity.isEmpty()) { response.sendError(401,"Cần đăng nhập quản trị.");return; }
        var user=identity.get();String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean writes=!request.getMethod().equals("GET");
        boolean allowed=path.startsWith("/api/admin/auth/") ||
            (path.startsWith("/api/admin/review")?user.role().equals("REVIEWER"):
             path.startsWith("/api/admin/content/") && writes?user.role().equals("EDITOR"):
             path.startsWith("/api/admin/content/") || path.startsWith("/api/admin/analytics") || path.startsWith("/api/admin/feedback")
                 || (path.equals("/api/admin/visitors") && request.getMethod().equals("GET")));
        if(!allowed) { response.sendError(403,"Vai trò không có quyền thực hiện thao tác này.");return; }
        request.setAttribute("adminIdentity",user);
        chain.doFilter(new HttpServletRequestWrapper(request) {
            public Principal getUserPrincipal() { return user::username; }
            public String getRemoteUser() { return user.username(); }
        },response);
    }
}
