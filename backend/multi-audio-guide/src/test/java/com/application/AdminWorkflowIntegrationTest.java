package com.application;

import com.application.repository.AdminContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "demo.admin.password=integration-only-password")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class AdminWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private String AUTH;
    @Autowired com.application.service.AdminAuthService authService;
    @org.junit.jupiter.api.BeforeEach void authenticate() {
        AUTH="Bearer "+authService.login(new com.application.dto.AdminAuthDto.Login("editor","EditorDemo!2026")).token();
    }
    private static final String LOCATION = "20000000-0000-0000-0000-000000000101";
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        r.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USERNAME"));
        r.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
    }
    @Test void namesPersistRejectedCommentIsReadOnlyAndPublishedLocationStaysIntact() throws Exception {
        String original = jdbc.queryForObject("SELECT name FROM locations WHERE id=?::uuid", String.class, LOCATION);
        String payload = """
                {"id":"%s","name":"Tên Việt","names":{"vi":"Tên Việt","en":"English name"},
                "category":"","latitude":"","longitude":"","radius":"","status":"DRAFT","assets":{}}
                """.formatted(LOCATION);
        mvc.perform(put("/api/admin/content/locations/" + LOCATION).header("Authorization", AUTH).contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.names.en").value("English name"));
        // External review only: the editor has no endpoint to assign this status/comment.
        jdbc.update("UPDATE admin_content_drafts SET status='REJECTED',reviewer_comment='Sửa bản dịch',edit_version=edit_version+1 WHERE location_id=?::uuid", LOCATION);
        mvc.perform(get("/api/admin/content/locations/" + LOCATION).header("Authorization", AUTH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.reviewerComment").value("Sửa bản dịch"));
        String category = jdbc.queryForObject("SELECT name FROM categories WHERE is_active ORDER BY name LIMIT 1", String.class);
        String resubmit = payload.replace("\"category\":\"\"", "\"category\":\"" + category + "\"")
                .replace("\"latitude\":\"\"", "\"latitude\":\"10\"").replace("\"longitude\":\"\"", "\"longitude\":\"106\"")
                .replace("\"radius\":\"\"", "\"radius\":\"80\"").replace("\"status\":\"DRAFT\"", "\"status\":\"PENDING_REVIEW\",\"editVersion\":2,\"reviewerComment\":\"forged\"");
        mvc.perform(put("/api/admin/content/locations/" + LOCATION).header("Authorization", AUTH).contentType("application/json").content(resubmit))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.reviewerComment").value("Sửa bản dịch")).andExpect(jsonPath("$.published.name").value(original));
        assertThat(jdbc.queryForObject("SELECT name FROM locations WHERE id=?::uuid", String.class, LOCATION)).isEqualTo(original);
    }
    @Test void catalogExecutesTwoBatchQueriesForOneAndManyLocations() {
        var template = spy(new NamedParameterJdbcTemplate(jdbc));
        var repository = new AdminContentRepository(template);
        assertThat(repository.workspaces(UUID.fromString(LOCATION))).hasSize(1);
        assertThat(mapQueryCount(template)).isEqualTo(2);
        clearInvocations(template);
        assertThat(repository.workspaces(null).size()).isGreaterThan(1);
        assertThat(mapQueryCount(template)).isEqualTo(2);
    }
    private long mapQueryCount(NamedParameterJdbcTemplate template) {
        return mockingDetails(template).getInvocations().stream().filter(i -> i.getMethod().getName().equals("query") &&
                i.getArguments().length == 3 && i.getArguments()[1] instanceof java.util.Map).count();
    }
    @Test void feedbackIsAuthenticatedFilteredPagedExportedAndAdvancedSequentially() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO feedbacks(id,location_id,category,content,rating,created_at,updated_at) VALUES (?,?::uuid,'Audio','Integration fixture',4,CURRENT_TIMESTAMP,'2020-01-01')", id, LOCATION);
        for (int n = 0; n < 12; n++) jdbc.update("INSERT INTO feedbacks(category,content,rating) VALUES ('Audio','Extra fixture',3)");
        jdbc.update("INSERT INTO feedbacks(category,content) VALUES ('Nội dung','Excluded fixture')");
        mvc.perform(get("/api/admin/feedback")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/feedback?category=Audio&status=NEW&page=1&size=10").header("Authorization", AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(10)).andExpect(jsonPath("$.total").value(13));
        mvc.perform(get("/api/admin/feedback/export?category=Audio&status=NEW").header("Authorization", AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(13));
        mvc.perform(patch("/api/admin/feedback/" + id + "/status").header("Authorization", AUTH).contentType("application/json").content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict());
        for (String state : new String[]{"IN_REVIEW", "RESOLVED"}) {
            mvc.perform(patch("/api/admin/feedback/" + id + "/status").header("Authorization", AUTH).contentType("application/json").content("{\"status\":\"" + state + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
        }
        assertThat(jdbc.queryForObject("SELECT updated_at>'2020-01-01'::timestamptz FROM feedbacks WHERE id=?", Boolean.class, id)).isTrue();
        mvc.perform(patch("/api/admin/feedback/" + id + "/status").header("Authorization", AUTH).contentType("application/json").content("{\"status\":\"NEW\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/feedback/export?category=Audio&status=RESOLVED").header("Authorization", AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/admin/feedback?from=2000-01-01&to=2000-01-02").header("Authorization", AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
}
