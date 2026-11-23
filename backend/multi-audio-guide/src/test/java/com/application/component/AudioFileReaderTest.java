package com.application.component;

import com.application.model.GuideContent;
import com.application.repository.ContentPackageRepository;
import com.application.service.ContentPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioFileReaderTest {
    @TempDir Path directory;

    @Test void packageManifestHashesLocalFileAndStillSupportsDatabaseOnlyAudio() throws Exception {
        var repository = mock(ContentPackageRepository.class);
        var storage = new AudioStorage(directory.toString());
        var reader = new AudioFileReader(repository, storage);
        var signer = new PackageSigner();
        var service = new ContentPackageService(repository, signer, reader);
        UUID id = UUID.randomUUID(), location = UUID.randomUUID();
        String url = "/api/content-audio/" + id + ".mp3";
        byte[] legacy = {1, 2}, file = {3, 4, 5};
        when(repository.audio(url)).thenReturn(legacy);
        assertThat(reader.read(url)).isEqualTo(legacy);
        storage.write(id, "mp3", file);
        when(repository.locations(true)).thenReturn(List.of(new com.application.dto.ContentPackageDto.Location(
                location, "Test", UUID.randomUUID(), "Category", 10, 106, 80)));
        when(repository.guides(true)).thenReturn(List.of(new GuideContent(
                UUID.randomUUID(), location, "Test", "VI", "FULL", "Test script", url)));
        var bundle = service.build("BASE");
        assertThat(bundle.manifest().assets()).singleElement().satisfies(asset -> {
            assertThat(asset.sizeBytes()).isEqualTo(file.length);
            assertThat(asset.sha256()).isEqualTo(signer.checksum(file));
        });
    }
}
