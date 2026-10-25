package com.application.controller;
import com.application.dto.TranslationDto.*;
import com.application.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo")
public class TranslationController {
    private final TranslationService service;
    public TranslationController(TranslationService service) { this.service=service; }
    @PostMapping("/api/admin/content/translate") public Result translate(@Valid @RequestBody Request request) { return service.translate(request); }
}
