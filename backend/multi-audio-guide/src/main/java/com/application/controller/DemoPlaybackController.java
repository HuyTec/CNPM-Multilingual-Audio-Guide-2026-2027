package com.application.controller;

import com.application.dto.AnalyticsResponse;
import com.application.dto.PlaybackReceipt;
import com.application.dto.PlaybackRequest;
import com.application.service.PlaybackService;
import com.application.service.TouristAuthService;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final TouristAuthService auth;
    public DemoPlaybackController(PlaybackService service, TouristAuthService auth) { this.service = service; this.auth=auth; }

    @PostMapping("/api/playbacks")
    public PlaybackReceipt record(@RequestBody PlaybackRequest request,
            @RequestHeader(value="Authorization",required=false) String token,
            @RequestHeader(value="X-Installation-Key",required=false) String key) {
        return service.record(request, auth.listener(token,key));
    }

    @GetMapping("/api/admin/analytics")
    public AnalyticsResponse analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.analytics(from, to);
    }
}
