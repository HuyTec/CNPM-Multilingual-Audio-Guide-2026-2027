package com.application.service;

import com.application.component.AudioFileReader;
import com.application.component.PackageSigner;
import com.application.dto.ContentPackageDto.*;
import com.application.model.GuideContent;
import com.application.repository.ContentPackageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Coordinates a single consistent published snapshot. */
@Service
@Profile("demo")
public class ContentPackageService {
    private final ContentPackageRepository repository;
    private final PackageSigner signer;
    private final AudioFileReader audio;
    private final com.application.repository.PublicationRepository versions;
    private final JsonMapper json = JsonMapper.builder().build();
    public ContentPackageService(ContentPackageRepository repository, PackageSigner signer, AudioFileReader audio) { this(repository,signer,audio,null); }
    @org.springframework.beans.factory.annotation.Autowired
    public ContentPackageService(ContentPackageRepository repository,PackageSigner signer,AudioFileReader audio,com.application.repository.PublicationRepository versions) {
        this.repository=repository;this.signer=signer;this.audio=audio;this.versions=versions;
    }
    public PublicKey publicKey() { return signer.publicKey(); }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Manifest> manifests() { return versions.manifests(); }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Bundle download(String scope, String version) {
        if (!List.of("BASE", "OPTIONAL", "WEB").contains(scope)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Loại gói không hợp lệ.");
        return versions.bundle(scope,version).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Chưa có phiên bản gói sẵn sàng."));
    }
    public record Preview(int locations, int guides, int audioFiles, Changes changes) { }
    public record Changes(long added, long updated, long removed) { }
    private record LocationContent(Location location, Map<GuideContent, Asset> audio, List<GuideContent> guides) { }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Preview preview(String scope, String version, String installedVersion) {
        var current = json.readValue(download(scope, version).bytes(), Payload.class);
        var changes = installedVersion == null ? null : versions.bundle(scope, installedVersion)
                .map(bundle -> compare(json.readValue(bundle.bytes(), Payload.class), current)).orElse(null);
        return new Preview(current.locations().size(), current.guides().size(), current.assets().size(), changes);
    }

    private Changes compare(Payload previous, Payload current) {
        var before = locationContent(previous);
        var after = locationContent(current);
        return new Changes(
                after.keySet().stream().filter(id -> !before.containsKey(id)).count(),
                after.keySet().stream().filter(id -> before.containsKey(id) && !after.get(id).equals(before.get(id))).count(),
                before.keySet().stream().filter(id -> !after.containsKey(id)).count());
    }

    private Map<java.util.UUID, LocationContent> locationContent(Payload payload) {
        var guides = payload.guides().stream().collect(Collectors.groupingBy(GuideContent::locationId));
        var assets = payload.assets().stream().collect(Collectors.toMap(Asset::url, Function.identity()));
        return payload.locations().stream().collect(Collectors.toMap(Location::locationId, location -> {
            var entries = guides.getOrDefault(location.locationId(), List.of());
            var audioFiles = entries.stream().filter(guide -> guide.audioUrl() != null)
                    .collect(Collectors.toMap(Function.identity(), guide -> assets.get(guide.audioUrl())));
            return new LocationContent(location, audioFiles, entries);
        }));
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Bundle build(String scope) {
        if (!List.of("BASE", "OPTIONAL", "WEB").contains(scope)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại gói không hợp lệ.");
        boolean base = !scope.equals("OPTIONAL");
        Boolean categoryPartition = scope.equals("WEB") ? null : base;
        var locations = repository.locations(categoryPartition); var guides = repository.guides(categoryPartition);
        // A freshly reset scope is valid and must not prevent publishing the other scope.
        // Keep rejecting a partial snapshot rather than publishing locations without guides.
        if (locations.isEmpty() != guides.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có gói đã duyệt.");
        List<Asset> assets = guides.stream().map(GuideContent::audioUrl).filter(url -> url != null).distinct().sorted().map(url -> {
            byte[] bytes = audio.read(url); return new Asset(url, bytes.length, signer.checksum(bytes));
        }).toList();
        var payload = new Payload(1, "APPROVED", scope, base, locations, guides,
                guides.stream().map(GuideContent::languageCode).distinct().sorted().toList(), assets, repository.geofencing(locations));
        byte[] bytes = json.writeValueAsBytes(payload);
        String checksum = signer.checksum(bytes), version = scope.toLowerCase(Locale.ROOT) + "-" + checksum;
        return new Bundle(new Manifest(1, scope, version, "APPROVED", base, bytes.length, checksum, signer.sign(bytes),
                signer.publicKey().keyId(), "/api/content-packages/" + scope + "/" + version, assets), bytes);
    }
}
