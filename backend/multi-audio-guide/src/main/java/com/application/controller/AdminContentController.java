package com.application.controller;

import com.application.service.AdminContentService;
import com.application.dto.AdminContentDto.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Existing DemoAdminFilter authenticates every /api/admin request. */
@RestController
@Profile("demo")
public class AdminContentController {
    private final AdminContentService service;
    public AdminContentController(AdminContentService service) { this.service=service; }
    @GetMapping("/api/admin/content/config") public Config config(){return service.config();}
    @GetMapping("/api/admin/content/locations") public List<LocationDetail> list(){return service.list();}
    @GetMapping("/api/admin/content/locations/{id}") public LocationDetail detail(@PathVariable UUID id){return service.detail(id);}
    @PutMapping("/api/admin/content/locations/{id}") public LocationDetail save(@PathVariable UUID id,@Valid @RequestBody SaveRequest body){return service.save(id,body);}
    @PostMapping(value="/api/admin/content/audio",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam UUID locationId,@RequestParam String language,@RequestParam String scriptType,@RequestParam MultipartFile file) throws IOException {
        return service.upload(locationId,language,scriptType,file.getOriginalFilename(),file.getBytes());
    }
    @GetMapping("/api/admin/content/audio/{id}.{format}") public ResponseEntity<byte[]> preview(@PathVariable UUID id,@PathVariable String format){return audio(id,format,true);}
    @GetMapping("/api/content-audio/{id}.{format}") public ResponseEntity<byte[]> published(@PathVariable UUID id,@PathVariable String format){return audio(id,format,false);}
    private ResponseEntity<byte[]> audio(UUID id,String format,boolean preview){
        var data=service.audio(id,format,preview);
        String type=switch(data.format()){case "mp3"->"audio/mpeg";case "ogg"->"audio/ogg";default->"audio/wav";};
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(type)).body(data.bytes());
    }
}
