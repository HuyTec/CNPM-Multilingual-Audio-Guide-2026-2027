package com.application;
import com.application.dto.AdminContentDto.*;
import com.application.dto.AdminAuthDto.Login;
import com.application.dto.PublicationDto.*;
import com.application.dto.TranslationDto;
import com.application.service.*;
import com.application.repository.PublicationRepository;
import com.application.component.PackageSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
@EnabledIfEnvironmentVariable(named="TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class PublicationIntegrationTest {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired AdminAuthService auth;
    @Autowired AdminContentService content;@Autowired ReviewService review;@Autowired PublicationService publisher;
    @Autowired PublicationRepository versions;@Autowired ContentPackageService packages;@Autowired PackageSigner signer;
    @Autowired TranslationService translation;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    private final JsonMapper json=JsonMapper.builder().build();
    @Autowired LocationImageService images;
    @Test void imageDraftPrivacyPublicationAndReplacement() throws Exception {
        UUID id=UUID.randomUUID();
        var output=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",output);
        var upload=images.upload(id,output.toByteArray(),"editor");
        String preview=upload.imageUrl().replace("/api/content-images/","/api/admin/content/images/");
        String editor="Bearer "+auth.login(new Login("editor","EditorDemo!2026")).token();
        mvc.perform(get(preview)).andExpect(status().isUnauthorized());
        mvc.perform(get(preview).header("Authorization",editor)).andExpect(status().isOk());
        mvc.perform(get(upload.imageUrl())).andExpect(status().isNotFound());
        Map<String,Asset> assets=Map.of("vi:FULL",new Asset(id,"vi","FULL","", "Bài đọc bằng TTS",null,null,null));
        var draft=content.save(id,new SaveRequest(id,"Ảnh thử",content.config().categories().getFirst(),"10.78","106.7","80","DRAFT",assets,0,Map.of("vi","Ảnh thử"),Map.of(),upload.imageUrl()));
        assertThat(content.detail(id).imageUrl()).isEqualTo(upload.imageUrl());
        draft=content.save(id,new SaveRequest(id,draft.name(),draft.category(),draft.latitude(),draft.longitude(),draft.radius(),"DRAFT",assets,draft.editVersion(),draft.names()));
        assertThat(draft.imageUrl()).isEqualTo(upload.imageUrl());
        var submitted=content.save(id,new SaveRequest(id,draft.name(),draft.category(),draft.latitude(),draft.longitude(),draft.radius(),"PENDING_REVIEW",assets,draft.editVersion(),draft.names(),Map.of(),draft.imageUrl()));
        var job=review.approve(id,new Decision(submitted.editVersion(),""),"reviewer");
        publisher.publish(job);
        mvc.perform(get(upload.imageUrl())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/locations/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.imageUrl").value(upload.imageUrl()));
        var replacement=images.upload(id,output.toByteArray(),"editor");
        var current=content.detail(id);
        content.save(id,new SaveRequest(id,current.name(),current.category(),current.latitude(),current.longitude(),current.radius(),"DRAFT",assets,current.editVersion(),current.names(),Map.of(),replacement.imageUrl()));
        mvc.perform(get(upload.imageUrl())).andExpect(status().isOk());
        mvc.perform(get(replacement.imageUrl())).andExpect(status().isNotFound());
        UUID other=UUID.randomUUID();
        assertThatThrownBy(()->content.save(other,new SaveRequest(other,"","","","","","DRAFT",Map.of(),0,Map.of(),Map.of(),upload.imageUrl())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->images.upload(id,"not an image".getBytes(),"editor"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->System.getenv("TEST_DATABASE_URL"));r.add("spring.datasource.username",()->System.getenv("TEST_DATABASE_USERNAME"));r.add("spring.datasource.password",()->System.getenv("TEST_DATABASE_PASSWORD"));
    }
    @Test void draftTranslationReviewReadyManifestAndVerifiedDownload() throws Exception {
        var initial=versions.enqueue(null);publisher.publish(initial);
        String editor="Bearer "+auth.login(new Login("editor","EditorDemo!2026")).token();
        String reviewer="Bearer "+auth.login(new Login("reviewer","ReviewerDemo!2026")).token();
        String before=mvc.perform(get("/api/content-packages")).andReturn().getResponse().getHeader("ETag");
        mvc.perform(get("/api/content-packages").header("If-None-Match",before)).andExpect(status().isNotModified());
        UUID id=UUID.randomUUID();
        String category=jdbc.queryForObject("SELECT name FROM categories WHERE id='10000000-0000-0000-0000-000000000101'",String.class);
        var translated=translation.translate(new TranslationDto.Request("vi",List.of("en"),"POI mới","Bài gốc","Bài ngắn"));
        var en=translated.variants().getFirst();
        var assets=Map.of("vi:FULL",new Asset(id,"vi","FULL","","Bài gốc",null,null,null),"en:FULL",new Asset(id,"en","FULL","",en.full(),null,null,null));
        var draft=content.save(id,new SaveRequest(id,"POI mới",category,"10.8","106.7","80","DRAFT",assets,0,Map.of("vi","POI mới","en",en.name()),Map.of("name:en",Origin.AUTO_TRANSLATED)));
        assertThat(draft.status()).isEqualTo("DRAFT");assertThat(content.detail(id).fieldOrigins().get("name:en")).isEqualTo(Origin.AUTO_TRANSLATED);
        var pending=content.save(id,new SaveRequest(id,draft.name(),category,"10.8","106.7","80","PENDING_REVIEW",assets,draft.editVersion(),draft.names(),draft.fieldOrigins()));
        mvc.perform(post("/api/admin/review/locations/"+id+"/approve").header("Authorization",editor).contentType("application/json").content(json.writeValueAsString(new Decision(pending.editVersion(),"OK")))).andExpect(status().isForbidden());
        String result=mvc.perform(post("/api/admin/review/locations/"+id+"/approve").header("Authorization",reviewer).contentType("application/json").content(json.writeValueAsString(new Decision(pending.editVersion(),"OK")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var job=json.readValue(result,Job.class);assertThat(job.status()).isEqualTo("BUILDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations WHERE id=?",Integer.class,id)).isZero();
        publisher.publish(job);assertThat(versions.job(job.id()).orElseThrow().status()).isEqualTo("READY");
        assertThat(content.detail(id).status()).isEqualTo("PUBLISHED");
        mvc.perform(get("/api/locations/"+id)).andExpect(jsonPath("$.names.en").value(en.name()));
        String after=mvc.perform(get("/api/content-packages").header("If-None-Match",before)).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");assertThat(after).isNotEqualTo(before);
        var manifest=packages.manifests().stream().filter(m->m.scope().equals("BASE")).findFirst().orElseThrow();
        byte[] bytes=mvc.perform(get(manifest.downloadUrl())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(signer.checksum(bytes)).isEqualTo(manifest.sha256());assertThat(signer.verify(bytes,manifest.signature())).isTrue();
        assertThat(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).contains(en.name());
        // Subsequent changes do not mutate previously downloaded immutable bytes.
        jdbc.update("UPDATE locations SET name='Another name' WHERE id=?",id);
        assertThat(packages.download(manifest.scope(),manifest.version()).bytes()).isEqualTo(bytes);
    }
    @Test void brokenAudioRollsBackPromotionAndKeepsReadyManifest() {
        var seed=versions.enqueue(null);publisher.publish(seed);
        var before=packages.manifests();
        UUID id=UUID.randomUUID();
        var asset=new Asset(id,"vi","FULL","","Nội dung", "/api/content-audio/"+UUID.randomUUID()+".mp3","missing.mp3","ready");
        var snapshot=new LocationDetail(id,"POI lỗi","Bảo tàng","10","106","80","APPROVED","",Map.of("vi:FULL",asset),2,null,null,null,Map.of("vi","POI lỗi"),null);
        var job=versions.enqueue(snapshot);
        var nested=new org.springframework.transaction.support.TransactionTemplate(transactions);
        nested.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(()->nested.execute(status->{new PublicationService(versions,packages,signer).publish(job);return null;})).hasMessageContaining("503");
        versions.fail(job.id(),"missing audio");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations WHERE id=?",Integer.class,id)).isZero();
        assertThat(packages.manifests()).isEqualTo(before);
        assertThat(versions.job(job.id()).orElseThrow().status()).isEqualTo("FAILED");
        assertThat(versions.retry(job.id())).isTrue();
    }
}
