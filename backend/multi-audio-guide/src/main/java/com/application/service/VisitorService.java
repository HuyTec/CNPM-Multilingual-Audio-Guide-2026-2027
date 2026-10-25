package com.application.service;

import com.application.dto.VisitorHeartbeat;
import com.application.dto.VisitorStats;
import com.application.repository.VisitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class VisitorService {
    private final VisitorRepository repository;
    public VisitorService(VisitorRepository repository) { this.repository = repository; }
    @Transactional
    public void heartbeat(VisitorHeartbeat request) { repository.touch(request); }
    @Transactional(readOnly = true)
    public VisitorStats stats() {
        Instant now = Instant.now();
        return new VisitorStats(now, 5, repository.stats(now.minusSeconds(300)));
    }
}
