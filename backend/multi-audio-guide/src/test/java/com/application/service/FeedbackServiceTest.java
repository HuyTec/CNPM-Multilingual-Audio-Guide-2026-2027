package com.application.service;

import com.application.dto.FeedbackDto.*;
import com.application.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FeedbackServiceTest {
    private final FeedbackRepository repository = mock(FeedbackRepository.class);
    private final FeedbackService service = new FeedbackService(repository);
    private final UUID id = UUID.randomUUID();
    private Item item(String status) { return new Item(id, null, "A", "Audio", "Text", 4, status, "2026-10-25", Instant.EPOCH, Instant.EPOCH); }
    @Test void skippingStepsAndReopeningAreRejectedWithoutWriting() {
        when(repository.find(id)).thenReturn(Optional.of(item("NEW")));
        assertThatThrownBy(() -> service.advance(id, new StatusRequest("RESOLVED"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        when(repository.find(id)).thenReturn(Optional.of(item("RESOLVED")));
        assertThatThrownBy(() -> service.advance(id, new StatusRequest("IN_REVIEW"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(repository, never()).advance(any(), anyString(), anyString());
    }
    @Test void validTransitionReturnsPersistedTimestamp() {
        when(repository.find(id)).thenReturn(Optional.of(item("NEW")), Optional.of(item("IN_REVIEW")));
        when(repository.advance(id, "NEW", "IN_REVIEW")).thenReturn(true);
        assertThat(service.advance(id, new StatusRequest("IN_REVIEW")).status()).isEqualTo("IN_REVIEW");
        verify(repository).advance(id, "NEW", "IN_REVIEW");
    }
    @Test void staleConcurrentTransitionIsAConflict() {
        when(repository.find(id)).thenReturn(Optional.of(item("NEW")));
        assertThatThrownBy(() -> service.advance(id, new StatusRequest("IN_REVIEW"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
    }
    @Test void invalidPeriodPaginationAndStatusAreRejectedBeforeReading() {
        assertThatThrownBy(() -> service.search(new Query(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 1), "", "", 1, 10))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(new Query(null, null, "", "INVALID", 1, 10))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(new Query(null, null, "", "", 0, 10))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repository);
    }
    @Test void exportPassesPeriodAndFiltersWithoutPaging() {
        service.export(new Query(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25), "Audio", "NEW", 4, 10));
        verify(repository).list(argThat(c -> c.start().equals(Instant.parse("2026-09-30T17:00:00Z")) &&
                c.end().equals(Instant.parse("2026-10-25T17:00:00Z")) && c.category().equals("Audio") && c.status().equals("NEW")), eq(true));
    }
}
