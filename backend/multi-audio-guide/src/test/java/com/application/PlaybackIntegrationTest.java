package com.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"demo.admin.username=test-admin", "demo.admin.password=test-only-password"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class PlaybackIntegrationTest {
    private static final String LOCATION = "20000000-0000-0000-0000-000000000001";
    private static final String CONTENT = "30000000-0000-0000-0000-000000000001";
    private static final String AUTH = "Basic " + Base64.getEncoder().encodeToString("test-admin:test-only-password".getBytes(StandardCharsets.UTF_8));
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
    }

    @Test void approvedShortGuideComesFromDatabaseAndFallsBackToVi() throws Exception {
        mvc.perform(get("/api/locations/" + LOCATION + "/guide").param("language", "JA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contentId").value(CONTENT))
                .andExpect(jsonPath("$.languageCode").value("VI"))
                .andExpect(jsonPath("$.scriptType").value("SHORT"))
                .andExpect(jsonPath("$.audioUrl").value("/demo-audio/museum.wav"));
    }

    @Test void draftContentIsNotPublic() throws Exception {
        jdbc.update("UPDATE guide_content_revisions SET status='DRAFT' WHERE id=?::uuid", CONTENT);
        mvc.perform(get("/api/locations/" + LOCATION + "/guide")).andExpect(status().isNotFound());
        record(UUID.randomUUID(), CONTENT, 404);
    }

    @Test void repeatedDeliveryCountsExactlyOnceAndDerivesLanguageFromContent() throws Exception {
        UUID event = UUID.randomUUID();
        mvc.perform(post("/api/playbacks").contentType(MediaType.APPLICATION_JSON).content(body(event, CONTENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(true));
        mvc.perform(post("/api/playbacks").contentType(MediaType.APPLICATION_JSON).content(body(event, CONTENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(false));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analytics_events WHERE id=?", Long.class, event)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT language_code FROM analytics_events WHERE id=?", String.class, event)).isEqualTo("VI");
    }

    @Test void replayIsANewEventButReusingIdForOtherContentIsAConflict() throws Exception {
        UUID event = UUID.randomUUID();
        record(event, CONTENT, 200);
        record(event, "30000000-0000-0000-0000-000000000002", 409);
        record(UUID.randomUUID(), CONTENT, 200);
    }

    @Test void missingOrUnknownContentIsRejected() throws Exception {
        mvc.perform(post("/api/playbacks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        record(UUID.randomUUID(), UUID.randomUUID().toString(), 404);
    }

    @Test void dashboardRequiresAdminCredentials() throws Exception {
        mvc.perform(get("/api/admin/analytics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, "Basic wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void calendarPeriodUsesVietnamMidnightAndInclusiveEndDate() throws Exception {
        jdbc.update("DELETE FROM analytics_events");
        for (String time : new String[]{"2026-10-18T16:59:59Z", "2026-10-18T17:00:00Z", "2026-10-19T16:59:59Z", "2026-10-19T17:00:00Z"}) {
            jdbc.update("INSERT INTO analytics_events(id,location_id,language_code,event_type,is_offline,occurred_at) VALUES (?,?::uuid,'VI','PLAYBACK_STARTED',false,?)",
                    UUID.randomUUID(), LOCATION, Timestamp.from(Instant.parse(time)));
        }
        mvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, AUTH)
                        .param("from", "2026-10-19").param("to", "2026-10-19"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPlaybacks").value(2))
                .andExpect(jsonPath("$.onlinePlaybacks").value(2)).andExpect(jsonPath("$.offlinePlaybacks").value(0))
                .andExpect(jsonPath("$.topLocations[0].count").value(2)).andExpect(jsonPath("$.languages[0].count").value(2));
    }

    @Test void emptyPeriodAndBadDatesAreHandled() throws Exception {
        mvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, AUTH)
                        .param("from", "1900-01-01").param("to", "1900-01-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPlaybacks").value(0))
                .andExpect(jsonPath("$.topLocations").isEmpty());
        mvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, AUTH)
                        .param("from", "2026-10-20").param("to", "2026-10-19"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, AUTH).param("from", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test void audioAssetIsRealWaveAndSupportsRangeRequests() throws Exception {
        byte[] wave = mvc.perform(get("/demo-audio/museum.wav")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(wave, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(wave.length).isGreaterThan(100_000);
        mvc.perform(get("/demo-audio/museum.wav").header("Range", "bytes=0-43"))
                .andExpect(status().isPartialContent()).andExpect(header().exists("Content-Range"));
    }

    private void record(UUID event, String content, int status) throws Exception {
        mvc.perform(post("/api/playbacks").contentType(MediaType.APPLICATION_JSON).content(body(event, content)))
                .andExpect(status().is(status));
    }
    private String body(UUID event, String content) {
        return "{\"eventId\":\"" + event + "\",\"contentId\":\"" + content + "\"}";
    }
}
