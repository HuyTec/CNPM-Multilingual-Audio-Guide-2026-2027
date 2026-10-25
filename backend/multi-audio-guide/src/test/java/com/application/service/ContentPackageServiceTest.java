package com.application.service;

import com.application.component.AudioFileReader;
import com.application.component.PackageSigner;
import com.application.dto.ContentPackageDto.Location;
import com.application.model.GuideContent;
import com.application.repository.ContentPackageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentPackageServiceTest {
    private final ContentPackageRepository repository = mock(ContentPackageRepository.class);
    private final AudioFileReader audio = mock(AudioFileReader.class);
    private final PackageSigner signer = new PackageSigner();
    private final ContentPackageService service = new ContentPackageService(repository, signer, audio);
    @Test void typedSnapshotRetainsClientFieldsAndVerifiableStableBytes() throws Exception {
        UUID location = UUID.randomUUID();
        var places = List.of(new Location(location, "Địa điểm", UUID.randomUUID(), "Văn hóa", 10, 106, 80));
        var guides = List.of(new GuideContent(UUID.randomUUID(), location, "Địa điểm", "VI", "SHORT", "Bài thuyết minh", "/demo-audio/museum.wav"));
        when(repository.locations(true)).thenReturn(places); when(repository.guides(true)).thenReturn(guides);
        when(audio.read("/demo-audio/museum.wav")).thenReturn(new byte[]{1, 2, 3});
        var bundle = service.build("BASE");
        var json = JsonMapper.builder().build().readTree(bundle.bytes());
        assertThat(json.get("isBase").asBoolean()).isTrue();
        assertThat(json.get("locations").get(0).get("geofenceRadiusM").asInt()).isEqualTo(80);
        assertThat(json.get("guides").get(0).get("languageCode").asString()).isEqualTo("VI");
        assertThat(json.has("geofencing")).isFalse();
        assertThat(service.build("BASE").bytes()).isEqualTo(bundle.bytes());
        assertThat(bundle.manifest().sha256()).isEqualTo(signer.checksum(bundle.bytes()));
        var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(service.publicKey().spki())));
        var verifier = Signature.getInstance("SHA256withRSA"); verifier.initVerify(key); verifier.update(bundle.bytes());
        assertThat(verifier.verify(Base64.getDecoder().decode(bundle.manifest().signature()))).isTrue();
    }
    @Test void invalidScopeAndMissingPublishedContentFailWithoutProducingPackage() {
        assertThatThrownBy(() -> service.build("INVALID")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(repository);
        when(repository.locations(true)).thenReturn(List.of()); when(repository.guides(true)).thenReturn(List.of());
        assertThatThrownBy(() -> service.build("BASE")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(audio);
    }
}
