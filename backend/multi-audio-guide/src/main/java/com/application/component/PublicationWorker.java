package com.application.component;
import com.application.repository.PublicationRepository;
import com.application.service.PublicationService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
@Component @Profile("demo") @EnableScheduling
public class PublicationWorker {
    private final PublicationRepository repository;private final PublicationService publisher;
    public PublicationWorker(PublicationRepository repository,PublicationService publisher) { this.repository=repository;this.publisher=publisher; }
    @Scheduled(fixedDelayString="${publication.poll-ms:3000}",initialDelay=3000)
    public void process() {
        for(var job:repository.pending()) try { publisher.publish(job); }
        catch(Exception error) { repository.fail(job.id(),"Tạo gói thất bại; hãy kiểm tra dữ liệu/audio và thử lại. "+error.getClass().getSimpleName()); }
    }
}
