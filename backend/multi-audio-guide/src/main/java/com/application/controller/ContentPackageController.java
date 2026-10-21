package com.application.controller;

import com.application.service.ContentPackageService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@Profile("demo")
public class ContentPackageController {
    private final ContentPackageService service;
    public ContentPackageController(ContentPackageService service) { this.service = service; }

    @GetMapping("/api/content-packages/key")
    public Map<String, String> key() { return service.publicKey(); }

    @GetMapping("/api/content-packages")
    public List<ContentPackageService.Manifest> packages() {
        return service.available();
    }

    @GetMapping("/api/content-packages/{scope}/{version}")
    public ResponseEntity<byte[]> download(@PathVariable String scope, @PathVariable String version) {
        var bundle = service.build(scope);
        if (!bundle.manifest().version().equals(version)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gói đã thay đổi. Hãy kiểm tra cập nhật lại.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
                .header("X-Content-SHA256", bundle.manifest().sha256()).body(bundle.bytes());
    }
}
