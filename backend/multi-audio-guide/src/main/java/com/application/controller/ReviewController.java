package com.application.controller;
import com.application.dto.AdminContentDto.LocationDetail;
import com.application.dto.PublicationDto.*;
import com.application.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;
@RestController @Profile("demo") @RequestMapping("/api/admin/review")
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service=service; }
    @GetMapping("/locations") public List<LocationDetail> queue() { return service.queue(); }
    @PostMapping("/locations/{id}/approve") public Job approve(@PathVariable UUID id,@Valid @RequestBody Decision request,Principal user) { return service.approve(id,request,user.getName()); }
    @PostMapping("/locations/{id}/reject") public LocationDetail reject(@PathVariable UUID id,@Valid @RequestBody Decision request,Principal user) { return service.reject(id,request,user.getName()); }
    @GetMapping("/jobs") public List<Job> jobs() { return service.jobs(); }
    @PostMapping("/jobs/{id}/retry") public Job retry(@PathVariable UUID id) { return service.retry(id); }
}
