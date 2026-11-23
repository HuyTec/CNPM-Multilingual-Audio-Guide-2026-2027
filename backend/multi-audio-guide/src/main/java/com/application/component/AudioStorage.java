package com.application.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("demo")
public class AudioStorage {
    private final Path root;

    public AudioStorage(
            @Value("${app.audio.storage-dir:./storage/audio}") String directory
    ) throws IOException {
        root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Chỉ tạo đường dẫn từ UUID và định dạng cho phép. */
    private Path resolve(UUID id, String format) {
        if (!Set.of("mp3", "wav", "ogg").contains(format)) {
            throw new IllegalArgumentException("Định dạng audio không hợp lệ.");
        }
        return root.resolve(Objects.requireNonNull(id) + "." + format);
    }

    public void write(UUID id, String format, byte[] bytes)
            throws IOException {
        Path target = resolve(id, format);
        Path temporary = Files.createTempFile(root, ".upload-", ".tmp");
        try {
            Files.write(temporary, bytes);
            // Publish a complete file; never replace an existing audio ID.
            Files.move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public byte[] read(UUID id, String format) throws IOException {
        return Files.readAllBytes(resolve(id, format));
    }

    /** Existing DB audio stays readable before a file has been created for it. */
    public byte[] readOrFallback(UUID id, String format, byte[] databaseBytes) throws IOException {
        try {
            return read(id, format);
        } catch (NoSuchFileException missing) {
            if (databaseBytes == null) throw missing;
            return databaseBytes;
        }
    }

    public void delete(UUID id, String format) throws IOException {
        Files.deleteIfExists(resolve(id, format));
    }
}
