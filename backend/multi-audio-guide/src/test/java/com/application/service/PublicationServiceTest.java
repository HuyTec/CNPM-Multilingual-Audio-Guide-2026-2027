package com.application.service;
import com.application.component.*;
import com.application.repository.PublicationRepository;
import com.application.dto.PublicationDto.Job;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
class PublicationServiceTest {
    @Test void failedBuildIsRecordedWithoutReplacingReadyPackage() {
        var repository=mock(PublicationRepository.class);var packages=mock(ContentPackageService.class);
        var job=new Job(UUID.randomUUID(),null,null,null,"BUILDING",null,null);
        when(repository.pending()).thenReturn(List.of(job));when(repository.job(job.id())).thenReturn(Optional.of(job));
        when(packages.build("BASE")).thenThrow(new IllegalStateException("missing audio"));
        var publisher=new PublicationService(repository,packages,new PackageSigner());
        new PublicationWorker(repository,publisher).process();
        verify(repository).fail(eq(job.id()),contains("thất bại"));
        verify(repository,never()).ready(any(),any());verify(repository,never()).complete(any());
    }
}
