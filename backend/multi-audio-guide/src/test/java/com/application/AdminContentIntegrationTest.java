package com.application;

import com.application.service.ContentPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="demo.admin.password=integration-only-password")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnabledIfEnvironmentVariable(named="TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
@Transactional
@SuppressWarnings("unchecked")
class AdminContentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ContentPackageService packages;
    private final JsonMapper json=JsonMapper.builder().build();
    private static final String ID="20000000-0000-0000-0000-000000000101";
    private static final String AUTH="Basic "+Base64.getEncoder().encodeToString("admin:integration-only-password".getBytes(StandardCharsets.UTF_8));
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->System.getenv("TEST_DATABASE_URL"));r.add("spring.datasource.username",()->System.getenv("TEST_DATABASE_USERNAME"));r.add("spring.datasource.password",()->System.getenv("TEST_DATABASE_PASSWORD"));
    }
    private Map<String,Object> detail(String id)throws Exception{return json.readValue(mvc.perform(get("/api/admin/content/locations/"+id).header("Authorization",AUTH)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),Map.class);}
    private Map<String,Object> save(Map<String,Object> data)throws Exception{return json.readValue(mvc.perform(put("/api/admin/content/locations/"+data.get("id")).header("Authorization",AUTH).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),Map.class);}
    private Map<String,Object> assets(Map<String,Object> data){return (Map<String,Object>)data.get("assets");}
    private Map<String,Object> asset(Map<String,Object> data,String key){return (Map<String,Object>)assets(data).get(key);}
    private String guide()throws Exception{return mvc.perform(get("/api/locations/"+ID+"/guide?language=FR&scriptType=SHORT")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();}

    @Test void authenticationAndRealSixLanguageWorkspace()throws Exception{
        mvc.perform(get("/api/admin/content/locations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/content/config").header("Authorization",AUTH)).andExpect(status().isOk()).andExpect(jsonPath("$.languages.length()").value(6));
        var location=detail(ID);assertThat(location.get("name")).isEqualTo("Dinh Độc Lập");assertThat(assets(location)).hasSize(12);assertThat(location).containsKey("published");
    }
    @Test void incompleteDraftPersistsButCannotBeSubmittedOrPublished()throws Exception{
        String id=UUID.randomUUID().toString();Map<String,Object> data=new LinkedHashMap<>(Map.of("id",id,"name","","category","","latitude","","longitude","","radius","","status","DRAFT","assets",Map.of()));
        var saved=save(data);assertThat(saved.get("editVersion")).isEqualTo(1);assertThat(saved.get("updatedBy")).isEqualTo("admin");assertThat(detail(id).get("status")).isEqualTo("DRAFT");
        mvc.perform(get("/api/locations/"+id+"/guide")).andExpect(status().isNotFound());
        saved.put("status","PENDING_REVIEW");mvc.perform(put("/api/admin/content/locations/"+id).header("Authorization",AUTH).contentType("application/json").content(json.writeValueAsString(saved))).andExpect(status().isBadRequest());
        saved.put("status","APPROVED");mvc.perform(put("/api/admin/content/locations/"+id).header("Authorization",AUTH).contentType("application/json").content(json.writeValueAsString(saved))).andExpect(status().isBadRequest());
        assertThat(detail(id).get("editVersion")).isEqualTo(1);
    }
    @Test void draftAndPendingKeepPublishedTextMetadataAndPackageUnchanged()throws Exception{
        String before=guide(),version=packages.build("BASE").manifest().version();var original=detail(ID);var working=detail(ID);
        working.put("name","Tên đang biên tập");asset(working,"fr:SHORT").put("text","Nội dung mới chỉ nằm trong bản làm việc.");working.put("status","DRAFT");var saved=save(working);
        assertThat(guide()).isEqualTo(before);saved.put("status","PENDING_REVIEW");saved=save(saved);assertThat(saved.get("status")).isEqualTo("PENDING_REVIEW");assertThat(saved.get("editVersion")).isEqualTo(2);
        assertThat(guide()).isEqualTo(before);assertThat(packages.build("BASE").manifest().version()).isEqualTo(version);
        assets(original).remove("fr:SHORT");var other=new LinkedHashMap<>(assets(saved));other.remove("fr:SHORT");
        // Text/audio identity for all eleven untouched variants is retained.
        for(var entry:assets(original).entrySet()){var a=(Map<String,Object>)entry.getValue();var b=(Map<String,Object>)other.get(entry.getKey());assertThat(b.get("text")).isEqualTo(a.get("text"));assertThat(b.get("audioUrl")).isEqualTo(a.get("audioUrl"));}
    }
    @Test void conflictsAndCrossVariantIdentityAreRejectedWithoutOverwriting()throws Exception{
        var data=detail(ID);data.put("status","DRAFT");save(data);
        mvc.perform(put("/api/admin/content/locations/"+ID).header("Authorization",AUTH).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isConflict());
        var current=detail(ID);asset(current,"vi:FULL").put("locationId",UUID.randomUUID().toString());
        mvc.perform(put("/api/admin/content/locations/"+ID).header("Authorization",AUTH).contentType("application/json").content(json.writeValueAsString(current))).andExpect(status().isBadRequest());assertThat(detail(ID).get("editVersion")).isEqualTo(1);
    }
    @Test void realAudioUploadIsPersistentPrivateAndChangesOnlyChosenVariant()throws Exception{
        int existingUploads=jdbc.queryForObject("SELECT count(*) FROM admin_audio_uploads",Integer.class);
        Object originalViAudio=asset(detail(ID),"vi:SHORT").get("audioUrl");
        byte[] wav=new ClassPathResource("static/demo-audio/museum.wav").getContentAsByteArray();
        var response=mvc.perform(multipart("/api/admin/content/audio").file(new MockMultipartFile("file","guide.wav","audio/wav",wav)).param("locationId",ID).param("language","FR").param("scriptType","SHORT").header("Authorization",AUTH)).andExpect(status().isOk()).andReturn().getResponse();
        Map<String,Object> upload=json.readValue(response.getContentAsString(),Map.class);String url=upload.get("audioUrl").toString();
        mvc.perform(get(url)).andExpect(status().isNotFound());mvc.perform(get(url.replace("/api/content-audio/","/api/admin/content/audio/"))).andExpect(status().isUnauthorized());
        mvc.perform(get(url.replace("/api/content-audio/","/api/admin/content/audio/")).header("Authorization",AUTH)).andExpect(status().isOk()).andExpect(content().bytes(wav));
        var working=detail(ID);working.put("status","DRAFT");asset(working,"fr:SHORT").putAll(upload);var saved=save(working);assertThat(asset(saved,"fr:SHORT").get("audioUrl")).isEqualTo(url);assertThat(asset(saved,"vi:SHORT").get("audioUrl")).isEqualTo(originalViAudio);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audio_uploads",Integer.class)).isEqualTo(existingUploads+1);assertThat(guide()).doesNotContain(url);
        // Only an external publication makes the stored audio public/packageable.
        UUID revision=UUID.randomUUID();jdbc.update("INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,script_text,status,revision_no) SELECT ?,?::uuid,'FR','SHORT',?,'APPROVED',max(revision_no)+1 FROM guide_content_revisions WHERE location_id=?::uuid AND language_code='FR' AND script_type='SHORT'",revision,ID,asset(saved,"fr:SHORT").get("text"),ID);
        jdbc.update("INSERT INTO audio_assets(content_id,storage_key,format) VALUES (?,?,'wav')",revision,url);
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(wav));
        assertThat(packages.build("BASE").manifest().assets()).anySatisfy(item->{assertThat(item.url()).isEqualTo(url);assertThat(item.sizeBytes()).isEqualTo(wav.length);});
    }
    @Test void invalidAudioHasNoPartialPersistence()throws Exception{
        int existingUploads=jdbc.queryForObject("SELECT count(*) FROM admin_audio_uploads",Integer.class);
        mvc.perform(multipart("/api/admin/content/audio").file(new MockMultipartFile("file","bad.wav","audio/wav","invalid audio file".getBytes(StandardCharsets.UTF_8))).param("locationId",ID).param("language","VI").param("scriptType","FULL").header("Authorization",AUTH)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audio_uploads",Integer.class)).isEqualTo(existingUploads);
    }
    @Test void externalPublicationAloneChangesSharedGuideAndPackage()throws Exception{
        var data=detail(ID);String before=guide(),oldVersion=packages.build("BASE").manifest().version();asset(data,"fr:SHORT").put("text","Nouvelle révision approuvée par le service externe.");data.put("status","PENDING_REVIEW");var pending=save(data);assertThat(guide()).isEqualTo(before);
        // Simulate the external publisher in this rolled-back DB test; editor API has no approve action.
        UUID content=UUID.randomUUID();jdbc.update("INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,script_text,status,revision_no) SELECT ?,?::uuid,'FR','SHORT',?,'APPROVED',max(revision_no)+1 FROM guide_content_revisions WHERE location_id=?::uuid AND language_code='FR' AND script_type='SHORT'",content,ID,asset(pending,"fr:SHORT").get("text"),ID);
        mvc.perform(get("/api/locations/"+ID+"/guide?language=FR&scriptType=SHORT")).andExpect(status().isOk()).andExpect(jsonPath("$.contentId").value(content.toString())).andExpect(jsonPath("$.scriptText").value(asset(pending,"fr:SHORT").get("text")));
        assertThat(packages.build("BASE").manifest().version()).isNotEqualTo(oldVersion);
    }
}
