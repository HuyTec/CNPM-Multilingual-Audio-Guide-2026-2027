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

/** Coordinates a single consistent published snapshot. */
@Service
@Profile("demo")
public class ContentPackageService {
    private final ContentPackageRepository repository;
    private final PackageSigner signer;
    private final AudioFileReader audio;
    private final JsonMapper json = JsonMapper.builder().build();
    public ContentPackageService(ContentPackageRepository repository, PackageSigner signer, AudioFileReader audio) {
        this.repository = repository; this.signer = signer; this.audio = audio;
    }
    public PublicKey publicKey() { return signer.publicKey(); }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Manifest> manifests() { return List.of(build("BASE").manifest(), build("OPTIONAL").manifest()); }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Bundle download(String scope, String version) {
        Bundle bundle = build(scope);
        if (!bundle.manifest().version().equals(version)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gói đã thay đổi. Hãy kiểm tra cập nhật lại.");
        return bundle;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Bundle build(String scope) {
        if (!List.of("BASE", "OPTIONAL").contains(scope)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại gói không hợp lệ.");
        boolean base = scope.equals("BASE");
        var locations = repository.locations(base); var guides = repository.guides(base);
        if (locations.isEmpty() || guides.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có gói đã duyệt.");
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
