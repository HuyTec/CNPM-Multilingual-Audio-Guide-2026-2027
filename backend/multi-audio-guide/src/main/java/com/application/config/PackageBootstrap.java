package com.application.config;
import com.application.repository.PublicationRepository;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Profile("demo")
public class PackageBootstrap implements ApplicationRunner {
    private final PublicationRepository repository;
    public PackageBootstrap(PublicationRepository repository) { this.repository=repository; }
    @Transactional public void run(ApplicationArguments args) {
        repository.lock();
        if(repository.manifests().isEmpty() && repository.jobs().isEmpty()) repository.enqueue(null);
    }
}
