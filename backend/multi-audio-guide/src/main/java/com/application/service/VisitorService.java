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
    public VisitorStats stats(int minutes) {
        Instant now = Instant.now();
        Instant activeSince = now.minusSeconds(minutes * 60L);

        return new VisitorStats(
            now,
            minutes,
            repository.stats(activeSince)
        );
    }
}
