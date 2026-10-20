package com.application.controller;

import com.application.dto.AnalyticsResponse;
import com.application.dto.PlaybackReceipt;
import com.application.dto.PlaybackRequest;
import com.application.service.PlaybackService;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Profile("demo")
public class DemoPlaybackController {
    private final PlaybackService service;
    public DemoPlaybackController(PlaybackService service) { this.service = service; }

    @PostMapping("/api/playbacks")
    public PlaybackReceipt record(@RequestBody PlaybackRequest request) { return service.record(request); }

    @GetMapping("/api/admin/analytics")
    public AnalyticsResponse analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.analytics(from, to);
    }
}
