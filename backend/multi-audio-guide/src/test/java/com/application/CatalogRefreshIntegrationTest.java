package com.application;

import com.application.service.ContentPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
@EnabledIfEnvironmentVariable(named="TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class CatalogRefreshIntegrationTest {
    private static final String LOCATION="20000000-0000-0000-0000-000000000108";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ContentPackageService packages;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",()->System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.username",()->System.getenv("TEST_DATABASE_USERNAME"));
        registry.add("spring.datasource.password",()->System.getenv("TEST_DATABASE_PASSWORD"));
    }
    @Test void retiredLocationsAndTheirStoredDataAreGone() throws Exception {
        for (int i=1;i<=3;i++) {
            String id="20000000-0000-0000-0000-"+String.format("%012d",i);
            mvc.perform(get("/api/locations/"+id)).andExpect(status().isNotFound());
            mvc.perform(get("/api/locations/"+id+"/guide")).andExpect(status().isNotFound());
            for (String table:new String[]{"guide_content_revisions","admin_audio_uploads","admin_content_drafts","location_image_uploads","feedbacks","analytics_events","publication_jobs"})
                assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE location_id=?::uuid",Integer.class,id)).as(table).isZero();
            assertThat(new String(packages.build("BASE").bytes(),java.nio.charset.StandardCharsets.UTF_8)).doesNotContain(id);
            assertThat(new String(packages.build("OPTIONAL").bytes(),java.nio.charset.StandardCharsets.UTF_8)).doesNotContain(id);
        }
    }
    @Test void xaLoiHasExactCoordinatesSixLanguagesBothModesAndMatchingVietnameseAudio() throws Exception {
        mvc.perform(get("/api/locations/"+LOCATION)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chùa Xá Lợi"))
                .andExpect(jsonPath("$.latitude").value(10.777973)).andExpect(jsonPath("$.longitude").value(106.686591));
        for (String language:new String[]{"VI","EN","FR","JA","ZH","RU"}) for (String type:new String[]{"FULL","SHORT"}) {
            var result=mvc.perform(get("/api/locations/"+LOCATION+"/guide").param("language",language).param("scriptType",type))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.languageCode").value(language))
                    .andExpect(jsonPath("$.scriptType").value(type)).andReturn();
            var guide=JsonMapper.builder().build().readTree(result.getResponse().getContentAsByteArray());
            assertThat(guide.get("scriptText").asString()).isNotBlank();
            if (language.equals("VI")) {
                byte[] audio=mvc.perform(get(guide.get("audioUrl").asString())).andExpect(status().isOk())
                        .andExpect(content().contentType("audio/mpeg")).andReturn().getResponse().getContentAsByteArray();
                assertThat(audio.length).isGreaterThan(1024);
                mvc.perform(get(guide.get("audioUrl").asString()).header("Range","bytes=100-199"))
                        .andExpect(status().isPartialContent())
                        .andExpect(header().string("Accept-Ranges","bytes"))
                        .andExpect(header().string("Content-Range","bytes 100-199/"+audio.length))
                        .andExpect(content().bytes(java.util.Arrays.copyOfRange(audio,100,200)));
                mvc.perform(get(guide.get("audioUrl").asString()).header("Range","bytes="+audio.length+"-"))
                        .andExpect(status().isRequestedRangeNotSatisfiable());
            }
        }
        String image=jdbc.queryForObject("SELECT image_url FROM locations WHERE id=?::uuid",String.class,LOCATION);
        mvc.perform(get(image)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM location_geofence_surveys WHERE location_id=?::uuid",Integer.class,LOCATION)).isZero();
    }
}
