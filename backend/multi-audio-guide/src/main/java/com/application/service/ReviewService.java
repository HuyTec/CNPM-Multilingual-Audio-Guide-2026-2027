package com.application.service;
import com.application.dto.AdminContentDto.LocationDetail;
import com.application.dto.PublicationDto.*;
import com.application.repository.PublicationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service @Profile("demo")
public class ReviewService {
    private final PublicationRepository repository;private final AdminContentService content;
    public ReviewService(PublicationRepository repository,AdminContentService content) { this.repository=repository;this.content=content; }
    public List<LocationDetail> queue() { return content.list().stream().filter(p->List.of("PENDING_REVIEW","APPROVED").contains(p.status())).toList(); }
    @Transactional public Job approve(UUID location,Decision decision,String reviewer) {
        var snapshot=content.detail(location);
        if(snapshot.assets().values().stream().noneMatch(a->a.text()!=null && !a.text().isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cần ít nhất một kịch bản trước khi duyệt.");
        if(!repository.decide(location,decision.editVersion(),"APPROVED",decision.comment(),reviewer)) conflict();
        return repository.enqueue(content.detail(location));
    }
    @Transactional public LocationDetail reject(UUID location,Decision decision,String reviewer) {
        if(decision.comment()==null || decision.comment().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cần nhận xét khi từ chối.");
        if(!repository.decide(location,decision.editVersion(),"REJECTED",decision.comment(),reviewer)) conflict();
        return content.detail(location);
    }
    public List<Job> jobs() { return repository.jobs(); }
    @Transactional public Job retry(UUID job) { if(!repository.retry(job)) conflict();return repository.job(job).orElseThrow(); }
    private static void conflict() { throw new ResponseStatusException(HttpStatus.CONFLICT,"Bản hoặc trạng thái đã thay đổi. Hãy tải lại."); }
}
