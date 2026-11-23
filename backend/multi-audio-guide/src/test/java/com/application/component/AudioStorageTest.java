package com.application.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioStorageTest {
    @TempDir Path directory;

    @Test void storesCompleteFilesAndDoesNotOverwriteAnExistingId() throws Exception {
        var storage = new AudioStorage(directory.resolve("audio").toString());
        UUID id = UUID.randomUUID();
        byte[] bytes = {1, 2, 3};
        storage.write(id, "mp3", bytes);
        assertThat(storage.read(id, "mp3")).isEqualTo(bytes);
        assertThatThrownBy(() -> storage.write(id, "mp3", new byte[]{4}))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(storage.read(id, "mp3")).isEqualTo(bytes);
        try (var files = Files.list(directory.resolve("audio"))) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactly(id + ".mp3");
        }
    }

    @Test void usesLegacyDatabaseBytesOnlyWhenFileIsMissing() throws Exception {
        var storage = new AudioStorage(directory.toString());
        UUID id = UUID.randomUUID();
        byte[] legacy = {1}, file = {2};
        assertThat(storage.readOrFallback(id, "wav", legacy)).isEqualTo(legacy);
        storage.write(id, "wav", file);
        assertThat(storage.readOrFallback(id, "wav", legacy)).isEqualTo(file);
        storage.delete(id, "wav");
        assertThat(storage.readOrFallback(id, "wav", legacy)).isEqualTo(legacy);
    }

    @Test void rejectsPathFragmentsAsFormats() throws Exception {
        var storage = new AudioStorage(directory.toString());
        assertThatThrownBy(() -> storage.write(UUID.randomUUID(), "../audio", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        try (var files = Files.list(directory)) { assertThat(files.count()).isZero(); }
    }
}
