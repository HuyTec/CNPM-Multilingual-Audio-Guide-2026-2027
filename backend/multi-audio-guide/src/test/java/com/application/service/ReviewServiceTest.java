package com.application.service;
import com.application.dto.AdminContentDto.*;
import com.application.dto.PublicationDto.*;
import com.application.repository.PublicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ReviewServiceTest {
    private final PublicationRepository repository=mock(PublicationRepository.class);
    private final AdminContentService content=mock(AdminContentService.class);
    private final ReviewService service=new ReviewService(repository,content);
    private final UUID id=UUID.randomUUID();
    @Test void rejectRequiresCommentAndDetectsStaleDecision() {
        assertThatThrownBy(()->service.reject(id,new Decision(1," "),"reviewer")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(repository);
        when(repository.decide(id,1,"REJECTED","Sửa nội dung","reviewer")).thenReturn(false);
        assertThatThrownBy(()->service.reject(id,new Decision(1,"Sửa nội dung"),"reviewer")).hasMessageContaining("409");
    }
    @Test void approveSnapshotsAcceptedVersionIntoDurableJob() {
        var asset=new Asset(id,"vi","FULL","","Bài",null,null,null);
        var snapshot=new LocationDetail(id,"Tên","Danh mục","10","106","80","PENDING_REVIEW","",Map.of("vi:FULL",asset),1,null,null,null);
        when(content.detail(id)).thenReturn(snapshot);
        when(repository.decide(id,1,"APPROVED","OK","reviewer")).thenReturn(true);
        var job=new Job(UUID.randomUUID(),id,2,snapshot,"BUILDING",null,null);
        when(repository.enqueue(snapshot)).thenReturn(job);
        assertThat(service.approve(id,new Decision(1,"OK"),"reviewer")).isEqualTo(job);
        verify(repository).enqueue(snapshot);
    }
    @Test void rejectionReturnsUpdatedEditorSnapshot() {
        when(repository.decide(id,3,"REJECTED","Cần sửa","reviewer")).thenReturn(true);
        service.reject(id,new Decision(3,"Cần sửa"),"reviewer");verify(content).detail(id);
    }
}
