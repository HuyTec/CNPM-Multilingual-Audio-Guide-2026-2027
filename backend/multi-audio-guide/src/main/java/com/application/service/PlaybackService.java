package com.application.service;

import com.application.dto.AnalyticsResponse;
import com.application.dto.PlaybackReceipt;
import com.application.dto.PlaybackRequest;
import com.application.model.GuideContent;
import com.application.repository.GuideRepository;
import com.application.repository.PlaybackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlaybackService {
    public static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final GuideRepository guides;
    private final PlaybackRepository playbacks;

    public PlaybackService(GuideRepository guides, PlaybackRepository playbacks) {
        this.guides = guides;
        this.playbacks = playbacks;
    }

    public GuideContent guide(UUID locationId, String language, String type) {
        String lang = language.trim().toUpperCase(Locale.ROOT);
        if (!lang.matches("[A-Z]{2,3}(-[A-Z]{2})?") || !(type.equals("SHORT") || type.equals("FULL"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngôn ngữ hoặc loại kịch bản không hợp lệ.");
        }
        return guides.find(locationId, lang, type).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Nội dung cho địa điểm này hiện đang được cập nhật."));
    }

    /** Same event UUID is safe to resend; reuse for a different content revision is rejected. */
    @Transactional
    public PlaybackReceipt record(PlaybackRequest request) {
        if (request.eventId() == null || request.contentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu eventId hoặc contentId.");
        }
        if ((request.installationId() == null) != (request.platform() == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "installationId và platform phải đi cùng nhau.");
        var existing = playbacks.contentForEvent(request.eventId());
        if (existing.isPresent()) {
            requireSameContent(existing.get(), request.contentId());
            return new PlaybackReceipt(request.eventId(), false);
        }
        Instant occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
        if (occurredAt.isAfter(Instant.now().plusSeconds(300))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời điểm nghe nằm trong tương lai. Hãy kiểm tra đồng hồ thiết bị.");
        }
        GuideContent guide = guides.findPublicById(request.contentId()).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Nội dung không khả dụng để ghi nhận lượt nghe."));
        boolean created = playbacks.insert(request.eventId(), guide, occurredAt, Boolean.TRUE.equals(request.offline()));
        if (created && request.installationId() != null)
            playbacks.attribute(request.eventId(), request.installationId(), request.platform().name());
        if (!created) requireSameContent(playbacks.contentForEvent(request.eventId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "eventId đã được sử dụng.")), request.contentId());
        return new PlaybackReceipt(request.eventId(), created);
    }

    private void requireSameContent(UUID existing, UUID requested) {
        if (!existing.equals(requested)) throw new ResponseStatusException(HttpStatus.CONFLICT, "eventId đã dùng cho nội dung khác.");
    }

    /** One database snapshot keeps totals and distributions consistent while new plays arrive. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalyticsResponse analytics(LocalDate from, LocalDate to) {
        LocalDate endDate = to == null ? LocalDate.now(REPORT_ZONE) : to;
        LocalDate startDate = from == null ? endDate.minusDays(29) : from;
        if (startDate.isAfter(endDate) || ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn kỳ báo cáo hợp lệ, tối đa 366 ngày.");
        }
        Instant start = startDate.atStartOfDay(REPORT_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
        long online = playbacks.count(start, end, false);
        long offline = playbacks.count(start, end, true);
        return new AnalyticsResponse(startDate, endDate, REPORT_ZONE.getId(), Instant.now(), online + offline,
                online, offline, playbacks.byLocation(start, end), playbacks.byLanguage(start, end));
    }
}
