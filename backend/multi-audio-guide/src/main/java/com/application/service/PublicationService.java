package com.application.service;
import com.application.component.PackageSigner;
import com.application.dto.PublicationDto.Job;
import com.application.repository.PublicationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Profile("demo")
public class PublicationService {
    private final PublicationRepository repository;private final ContentPackageService packages;private final PackageSigner signer;
    public PublicationService(PublicationRepository repository,ContentPackageService packages,PackageSigner signer) { this.repository=repository;this.packages=packages;this.signer=signer; }
    /** Public promotion and both verified package snapshots commit together, or all roll back. */
    @Transactional public void publish(Job candidate) {
        repository.lock();
        var job=repository.job(candidate.id()).orElseThrow();
        if(!job.status().equals("BUILDING")) return;
        if(job.payload()!=null) repository.promote(job.payload());
        for(String scope:java.util.List.of("BASE","OPTIONAL")) {
            var bundle=packages.build(scope);
            if(!signer.checksum(bundle.bytes()).equals(bundle.manifest().sha256()) || !signer.verify(bundle.bytes(),bundle.manifest().signature())) throw new IllegalStateException("Gói không qua xác minh.");
            repository.ready(job.id(),bundle);
        }
        repository.complete(job);
    }
}
