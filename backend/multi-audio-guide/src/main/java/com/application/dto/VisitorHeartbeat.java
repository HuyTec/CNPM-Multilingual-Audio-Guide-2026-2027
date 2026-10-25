package com.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VisitorHeartbeat(@NotNull UUID installationId, @NotNull Platform platform) {
    public enum Platform { WEB, ANDROID }
}
