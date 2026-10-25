package com.application;

import com.application.dto.AdminAuthDto.Login;
import com.application.service.AdminAuthService;
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
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
@EnabledIfEnvironmentVariable(named="TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class VisitorIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminAuthService auth;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->System.getenv("TEST_DATABASE_URL"));
        r.add("spring.datasource.username",()->System.getenv("TEST_DATABASE_USERNAME"));
        r.add("spring.datasource.password",()->System.getenv("TEST_DATABASE_PASSWORD"));
    }
    @Test void repeatedHeartbeatCountsOneInstallationAndActiveWindowExpires() throws Exception {
        jdbc.update("DELETE FROM visitor_installations");
        UUID android = UUID.randomUUID(), web = UUID.randomUUID();
        heartbeat(android,"ANDROID"); heartbeat(android,"ANDROID"); heartbeat(web,"WEB");
        jdbc.update("UPDATE visitor_installations SET last_seen_at=CURRENT_TIMESTAMP-INTERVAL '6 minutes' WHERE installation_id=?",web);
        String bearer="Bearer "+auth.login(new Login("editor","EditorDemo!2026")).token();
        mvc.perform(get("/api/admin/visitors")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/visitors").header("Authorization",bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.activeWindowMinutes").value(5))
            .andExpect(jsonPath("$.platforms[0].platform").value("ANDROID"))
            .andExpect(jsonPath("$.platforms[0].totalInstallations").value(1))
            .andExpect(jsonPath("$.platforms[0].activeInstallations").value(1))
            .andExpect(jsonPath("$.platforms[1].activeInstallations").value(0));
        mvc.perform(post("/api/visitors/heartbeat").contentType("application/json").content("{\"platform\":\"ANDROID\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void phonePlaybackIsCountedOnceAndRetainsOfflineTimeAndPlatform() throws Exception {
        UUID event=UUID.randomUUID(), installation=UUID.randomUUID();
        UUID content=jdbc.queryForObject("SELECT id FROM guide_content_revisions WHERE status='APPROVED' LIMIT 1",UUID.class);
        String body="""
            {"eventId":"%s","contentId":"%s","installationId":"%s","platform":"ANDROID",
             "occurredAt":"2026-01-01T12:00:00Z","offline":true}
            """.formatted(event,content,installation);
        mvc.perform(post("/api/playbacks").contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(true));
        mvc.perform(post("/api/playbacks").contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(false));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analytics_events WHERE id=? AND client_platform='ANDROID' AND is_offline=true AND installation_id=?",Long.class,event,installation)).isEqualTo(1);
    }
    private void heartbeat(UUID id,String platform) throws Exception {
        mvc.perform(post("/api/visitors/heartbeat").contentType("application/json")
            .content("{\"installationId\":\""+id+"\",\"platform\":\""+platform+"\"}"))
            .andExpect(status().isNoContent());
    }
}
