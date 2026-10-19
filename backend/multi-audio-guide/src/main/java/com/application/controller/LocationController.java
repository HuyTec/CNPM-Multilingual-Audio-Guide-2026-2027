package com.application.controller;

import com.application.dto.CategoryResponse;
import com.application.dto.LocationResponse;
import com.application.dto.LocationSearchResponse;
import com.application.service.LocationDiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LocationController {
    private final LocationDiscoveryService service;

    public LocationController(LocationDiscoveryService service) {
        this.service = service;
    }

    @GetMapping("/locations")
    public LocationSearchResponse search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(keyword, categoryId, page, size);
    }

    @GetMapping("/locations/{locationId}")
    public LocationResponse detail(@PathVariable UUID locationId) {
        return service.detail(locationId);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return service.categories();
    }
}
