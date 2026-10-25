package com.application.controller;

import com.application.service.LocationImageService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@RestController @Profile("demo")
public class LocationImageController {
    private final LocationImageService service;
    public LocationImageController(LocationImageService service) { this.service=service; }
    @PostMapping(value="/api/admin/content/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public LocationImageService.Upload upload(@RequestParam UUID locationId,@RequestParam MultipartFile file,Principal actor) throws IOException {
        return service.upload(locationId,file.getBytes(),actor.getName());
    }
    @GetMapping("/api/admin/content/images/{id}.png")
    public ResponseEntity<byte[]> preview(@PathVariable UUID id) { return image(id,true); }
    @GetMapping("/api/content-images/{id}.png")
    public ResponseEntity<byte[]> published(@PathVariable UUID id) { return image(id,false); }
    private ResponseEntity<byte[]> image(UUID id,boolean preview) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .contentType(MediaType.IMAGE_PNG).body(service.read(id,preview));
    }
}
