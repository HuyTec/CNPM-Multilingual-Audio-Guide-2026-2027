package com.application.controller;

import com.application.service.ContentPackageService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.application.dto.ContentPackageDto.PublicKey;
import com.application.dto.ContentPackageDto.Manifest;

@RestController
@Profile("demo")
public class ContentPackageController {
    private final ContentPackageService service;
    public ContentPackageController(ContentPackageService service) { this.service = service; }

    @GetMapping("/api/content-packages/key")
    public PublicKey key() { return service.publicKey(); }

    @GetMapping("/api/content-packages")
    public List<Manifest> packages() {
        return service.manifests();
    }

    @GetMapping("/api/content-packages/{scope}/{version}")
    public ResponseEntity<byte[]> download(@PathVariable String scope, @PathVariable String version) {
        var bundle = service.download(scope, version);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
                .header("X-Content-SHA256", bundle.manifest().sha256()).body(bundle.bytes());
    }
}
