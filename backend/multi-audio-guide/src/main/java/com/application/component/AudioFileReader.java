package com.application.component;

import com.application.repository.ContentPackageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Only reads allowlisted classpath/database sources; never fetches remote audio. */
@Component
@Profile("demo")
public class AudioFileReader {
    private final ContentPackageRepository repository;
    public AudioFileReader(ContentPackageRepository repository) { this.repository = repository; }
    public byte[] read(String url) {
        try {
            byte[] bytes;
            if (url.matches("/demo-audio/[a-z-]+\\.wav")) bytes = new ClassPathResource("static" + url).getContentAsByteArray();
            else if (url.matches("/api/content-audio/[0-9a-f-]{36}\\.(mp3|wav|ogg)")) bytes = repository.audio(url);
            else throw new IllegalArgumentException("Unsupported audio source");
            if (bytes == null) throw new IllegalStateException("Missing audio");
            return bytes;
        } catch (Exception error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể chuẩn bị audio cho gói."); }
    }
}
