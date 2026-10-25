package com.application.controller;

import com.application.dto.VisitorHeartbeat;
import com.application.dto.VisitorStats;
import com.application.service.VisitorService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("demo")
public class VisitorController {
    private final VisitorService service;
    public VisitorController(VisitorService service) { this.service = service; }
    @PostMapping("/api/visitors/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@Valid @RequestBody VisitorHeartbeat request) { service.heartbeat(request); }
    @GetMapping("/api/admin/visitors")
    public VisitorStats stats() { return service.stats(); }
}
