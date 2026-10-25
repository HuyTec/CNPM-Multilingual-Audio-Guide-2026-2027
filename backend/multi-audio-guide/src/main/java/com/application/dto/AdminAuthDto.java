package com.application.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
public final class AdminAuthDto {
    private AdminAuthDto() { }
    public record Login(@NotBlank @Size(max=100) String username,@NotBlank @Size(max=200) String password) { }
    public record Account(String username,String passwordHash,String role) { }
    public record Identity(String username,String role) { }
    public record Session(String token,String username,String role,Instant expiresAt) { }
}
