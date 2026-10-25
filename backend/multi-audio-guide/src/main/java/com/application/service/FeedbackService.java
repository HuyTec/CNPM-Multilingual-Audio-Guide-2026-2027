package com.application.service;

import com.application.dto.FeedbackDto.*;
import com.application.repository.FeedbackRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** UC-07 consumes externally submitted feedback; RESOLVED is final. */
@Service
@Profile("demo")
public class FeedbackService {
    private final FeedbackRepository repository;
    public FeedbackService(FeedbackRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page search(Query query) {
        Criteria c = criteria(query); long total = repository.count(c);
        return new Page(repository.list(c, false), c.page(), c.size(), total,
                total / c.size() + (total % c.size() == 0 ? 0 : 1), repository.categories());
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Item> export(Query query) { return repository.list(criteria(query), true); }
    @Transactional(readOnly = true)
    public Item detail(UUID id) {
        return repository.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phản hồi."));
    }
    @Transactional
    public Item advance(UUID id, StatusRequest request) {
        Item current = detail(id);
        String next = switch (current.status()) { case "NEW" -> "IN_REVIEW"; case "IN_REVIEW" -> "RESOLVED"; default -> ""; };
        if (next.isEmpty() || !next.equals(request.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ được chuyển NEW → IN_REVIEW → RESOLVED; phản hồi hoàn tất không mở lại.");
        if (!repository.advance(id, current.status(), next))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phản hồi đã đổi. Tải lại chi tiết rồi thử lại.");
        return detail(id);
    }
    private Criteria criteria(Query query) {
        LocalDate to = query.to() == null ? LocalDate.now(PlaybackService.REPORT_ZONE) : query.to();
        LocalDate from = query.from() == null ? to.minusDays(29) : query.from();
        String category = query.category() == null ? "" : query.category().trim();
        String status = query.status() == null ? "" : query.status();
        int page = query.page() == null ? 1 : query.page(), size = query.size() == null ? 10 : query.size();
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365 || category.length() > 100 ||
                !List.of("", "NEW", "IN_REVIEW", "RESOLVED").contains(status) || page < 1 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kỳ báo cáo tối đa 366 ngày; bộ lọc, trang hoặc kích thước trang không hợp lệ.");
        return new Criteria(from.atStartOfDay(PlaybackService.REPORT_ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(PlaybackService.REPORT_ZONE).toInstant(), category, status, page, size);
    }
}
