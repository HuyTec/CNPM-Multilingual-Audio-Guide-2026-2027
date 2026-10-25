package com.application.service;
import com.application.component.TranslationProvider;
import com.application.dto.TranslationDto.*;
import com.application.repository.AdminContentRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class TranslationServiceTest {
    @Test void providerFailureDoesNotWriteOrMutateDraft() {
        var provider=mock(TranslationProvider.class);var repository=mock(AdminContentRepository.class);
        when(repository.languageActive(anyString())).thenReturn(true);
        when(provider.translate(anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("timeout"));
        var source=new Request("vi",List.of("en"),"Tên","Nội dung","Ngắn");
        assertThatThrownBy(()->new TranslationService(provider,repository).translate(source)).hasMessageContaining("502");
        assertThat(source.full()).isEqualTo("Nội dung");verify(repository,never()).save(any(),anyInt(),anyString());
    }
    @Test void simulatedProviderIsClearlyIdentified() {
        var repository=mock(AdminContentRepository.class);when(repository.languageActive(anyString())).thenReturn(true);
        var service=new TranslationService(new com.application.component.DemoTranslationProvider(),repository);
        var result=service.translate(new Request("vi",List.of("en"),"Tên","Bài","Ngắn"));
        assertThat(result.simulated()).isTrue();assertThat(result.variants().getFirst().name()).isEqualTo("[DEMO en] Tên");
    }
}
