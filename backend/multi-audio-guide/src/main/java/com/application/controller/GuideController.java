package com.application.controller;

import com.application.model.GuideContent;
import com.application.service.PlaybackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class GuideController {
    private final PlaybackService service;

    public GuideController(PlaybackService service) { this.service = service; }

    @GetMapping("/api/locations/{id}/guide")
    public GuideContent guide(@PathVariable UUID id, @RequestParam(defaultValue = "VI") String language,
                              @RequestParam(defaultValue = "FULL") String scriptType) {
        return service.guide(id, language, scriptType);
    }
}
