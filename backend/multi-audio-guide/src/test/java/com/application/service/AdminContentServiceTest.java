package com.application.service;

import com.application.dto.AdminContentDto.*;
import com.application.component.AudioStorage;
import com.application.repository.AdminContentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminContentServiceTest {
    private final AdminContentRepository repository = mock(AdminContentRepository.class);
    private final AudioStorage storage = mock(AudioStorage.class);
    private final AdminContentService service = new AdminContentService(repository, "editor", null, storage);
    private final UUID id = UUID.randomUUID();
    private byte[] wav() { return "RIFF0000WAVEaudio".getBytes(java.nio.charset.StandardCharsets.US_ASCII); }

    @Test void uploadWritesFileAndKeepsDatabaseBytes() throws Exception {
        when(repository.languageActive("VI")).thenReturn(true);
        byte[] bytes = wav();
        var result = service.upload(id, "vi", "FULL", "guide.wav", bytes);
        var audioId = ArgumentCaptor.forClass(UUID.class);
        verify(storage).write(audioId.capture(), eq("wav"), eq(bytes));
        verify(repository).insertAudio(audioId.getValue(), id, "VI", "FULL", "guide.wav", "wav", bytes, "editor");
        assertThat(result.audioUrl()).isEqualTo("/api/content-audio/" + audioId.getValue() + ".wav");
    }
    @Test void failedFileWriteDoesNotInsertDatabaseRow() throws Exception {
        when(repository.languageActive("VI")).thenReturn(true);
        doThrow(new IOException("disk unavailable")).when(storage).write(any(), eq("wav"), any());
        assertThatThrownBy(() -> service.upload(id, "VI", "FULL", "guide.wav", wav()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        verify(repository, never()).insertAudio(any(), any(), anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }
    @Test void databaseFailureRemovesTheNewFile() throws Exception {
        when(repository.languageActive("VI")).thenReturn(true);
        doThrow(new IllegalStateException("database unavailable")).when(repository)
                .insertAudio(any(), any(), anyString(), anyString(), anyString(), anyString(), any(), anyString());
        assertThatThrownBy(() -> service.upload(id, "VI", "FULL", "guide.wav", wav()))
                .isInstanceOf(IllegalStateException.class);
        var audioId = ArgumentCaptor.forClass(UUID.class);
        verify(storage).write(audioId.capture(), eq("wav"), any());
        verify(storage).delete(audioId.getValue(), "wav");
    }
    @Test void transactionRollbackRemovesTheNewFile() throws Exception {
        when(repository.languageActive("VI")).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(id, "VI", "FULL", "guide.wav", wav());
            var audioId = ArgumentCaptor.forClass(UUID.class);
            verify(storage).write(audioId.capture(), eq("wav"), any());
            for (var synchronization : TransactionSynchronizationManager.getSynchronizations())
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(storage).delete(audioId.getValue(), "wav");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void unpublishedAudioCannotBeReadFromDisk() {
        when(repository.audio(id, "wav", false)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.audio(id, "wav", false))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(storage);
    }
    @Test void previewUsesStorageOnlyAfterDatabaseAuthorization() throws Exception {
        byte[] oldBytes = wav(), fileBytes = {1, 2, 3};
        when(repository.audio(id, "wav", true)).thenReturn(java.util.Optional.of(new Audio(oldBytes, "wav")));
        when(storage.readOrFallback(id, "wav", oldBytes)).thenReturn(fileBytes);
        assertThat(service.audio(id, "wav", true).bytes()).isEqualTo(fileBytes);
    }
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
