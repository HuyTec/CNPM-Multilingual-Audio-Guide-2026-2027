package com.application.controller;

import com.application.dto.FeedbackDto.*;
import com.application.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("demo")
@RequestMapping("/api/admin/feedback")
public class FeedbackController {
    private final FeedbackService service;
    public FeedbackController(FeedbackService service) { this.service = service; }
    @GetMapping public Page list(@ModelAttribute Query query) { return service.search(query); }
    @GetMapping("/export") public List<Item> export(@ModelAttribute Query query) { return service.export(query); }
    @GetMapping("/{id}") public Item detail(@PathVariable UUID id) { return service.detail(id); }
    @PatchMapping("/{id}/status") public Item advance(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return service.advance(id, request);
    }
}
