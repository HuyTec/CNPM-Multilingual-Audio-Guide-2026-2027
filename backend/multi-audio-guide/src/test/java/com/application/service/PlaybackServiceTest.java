package com.application.service;

import com.application.dto.PlaybackRequest;
import com.application.model.GuideContent;
import com.application.repository.GuideRepository;
import com.application.repository.PlaybackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlaybackServiceTest {
    private final GuideRepository guides = mock(GuideRepository.class);
    private final PlaybackRepository events = mock(PlaybackRepository.class);
    private final PlaybackService service = new PlaybackService(guides, events);
    private final UUID id = UUID.randomUUID();
    private final GuideContent guide = new GuideContent(UUID.randomUUID(), UUID.randomUUID(), "Fixture", "VI", "FULL", "Text", null);

    @Test void delayedOfflineReceiptRetainsOriginalTimeAndFlag() {
        Instant occurred = Instant.parse("2026-01-01T12:00:00Z");
        when(guides.findPublicById(guide.contentId())).thenReturn(Optional.of(guide));
        when(events.insert(id, guide, occurred, true)).thenReturn(true);
        assertThat(service.record(new PlaybackRequest(id, guide.contentId(), occurred, true)).created()).isTrue();
        verify(events).insert(id, guide, occurred, true);
    }

    @Test void legacyClientUsesCurrentTimeAndOnline() {
        Instant before = Instant.now();
        when(guides.findPublicById(guide.contentId())).thenReturn(Optional.of(guide));
        when(events.insert(eq(id), eq(guide), any(Instant.class), eq(false))).thenReturn(true);
        service.record(new PlaybackRequest(id, guide.contentId()));
        verify(events).insert(eq(id), eq(guide), argThat(value -> !value.isBefore(before) && !value.isAfter(Instant.now())), eq(false));
    }

    @Test void replayNeverChangesStoredMetadataAndNeedsNoLongerPublishedContent() {
        when(events.contentForEvent(id)).thenReturn(Optional.of(guide.contentId()));
        assertThat(service.record(new PlaybackRequest(id, guide.contentId(), Instant.now(), false)).created()).isFalse();
        verifyNoInteractions(guides);
        verify(events, never()).insert(any(), any(), any(), anyBoolean());
    }

    @Test void futureClockIsRejectedBeforeInsert() {
        assertThatThrownBy(() -> service.record(new PlaybackRequest(id, guide.contentId(), Instant.now().plusSeconds(3600), true)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(guides);
    }
}
