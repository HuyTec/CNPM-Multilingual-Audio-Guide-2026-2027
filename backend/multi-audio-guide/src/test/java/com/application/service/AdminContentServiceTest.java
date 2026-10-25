package com.application.service;

import com.application.dto.AdminContentDto.*;
import com.application.repository.AdminContentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminContentServiceTest {
    private final AdminContentRepository repository = mock(AdminContentRepository.class);
    private final AdminContentService service = new AdminContentService(repository, "editor");
    private final UUID id = UUID.randomUUID();
    private SaveRequest request(String status, String name, String category, String lat, String lng, String radius) {
        return new SaveRequest(id, name, category, lat, lng, radius, status, Map.of(), 1);
    }
    private LocationDetail location(String status, String name) {
        return new LocationDetail(id, name, "Văn hóa", "10", "106", "80", status, "APPROVED", Map.of(), 1, "editor", null, null);
    }
    @Test void incompleteDraftIsSavedThroughRepositoryWithoutDatabase() {
        when(repository.save(any(), eq(1), eq("editor"))).thenReturn(true);
        when(repository.workspaces(id)).thenReturn(List.of(new Workspace(null, location("DRAFT", ""))));
        assertThat(service.save(id, request("DRAFT", "", "", "", "", "")).status()).isEqualTo("DRAFT");
        var payload = ArgumentCaptor.forClass(LocationDetail.class);
        verify(repository).save(payload.capture(), eq(1), eq("editor"));
        assertThat(payload.getValue().name()).isEmpty();
    }
    @Test void incompleteSubmissionAndEditorApprovalAreRejectedBeforeWrite() {
        assertThatThrownBy(() -> service.save(id, request("PENDING_REVIEW", "", "", "", "", ""))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.save(id, request("APPROVED", "A", "", "", "", ""))).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any(), anyInt(), anyString());
    }
    @Test void staleVersionFailsRatherThanOverwriting() {
        when(repository.categoryActive("Văn hóa")).thenReturn(true);
        assertThatThrownBy(() -> service.save(id, request("PENDING_REVIEW", "A", "Văn hóa", "10", "106", "80")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
    }
    @Test void workingDraftRetainsApprovedSnapshot() {
        when(repository.workspaces(null)).thenReturn(List.of(new Workspace(location("APPROVED", "Published"), location("DRAFT", "Working"))));
        var result = service.list().getFirst();
        assertThat(result.name()).isEqualTo("Working");
        assertThat(result.published().name()).isEqualTo("Published");
        verify(repository).workspaces(null);
    }
    @Test void localizedNamesArePersistedAndDefaultFallsBackToEnglish() {
        when(repository.languageActive("VI")).thenReturn(true);
        when(repository.languageActive("EN")).thenReturn(true);
        when(repository.save(any(), eq(1), eq("editor"))).thenReturn(true);
        when(repository.workspaces(id)).thenReturn(List.of(new Workspace(null, location("DRAFT", "English name"))));
        service.save(id, new SaveRequest(id, "English name", "", "", "", "", "DRAFT", Map.of(), 1, Map.of("vi", "", "en", "English name")));
        var payload = ArgumentCaptor.forClass(LocationDetail.class);
        verify(repository).save(payload.capture(), eq(1), eq("editor"));
        assertThat(payload.getValue().name()).isEqualTo("English name");
        assertThat(payload.getValue().names()).containsEntry("vi", "").containsEntry("en", "English name");
    }
}
