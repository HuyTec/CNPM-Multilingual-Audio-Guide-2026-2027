package com.application;

import com.application.repository.GuideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL/PostGIS, real migrations and HTTP stack, with no demo profile. */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class GuideIntegrationTest {
    private static final String LOCATION = "20000000-0000-0000-0000-000000000001";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired GuideRepository guides;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
    }

    @ParameterizedTest
    @CsvSource({"VI,SHORT,1", "VI,FULL,4", "EN,FULL,5", "EN,SHORT,6"})
    void fourVariantsReturnExactStoredTextAndMatchingOptionalAudio(String language, String type, int suffix) throws Exception {
        String id = "30000000-0000-0000-0000-00000000000" + suffix;
        String text = jdbc.queryForObject("SELECT script_text FROM guide_content_revisions WHERE id=?::uuid", String.class, id);
        var result = mvc.perform(get("/api/locations/{id}/guide", LOCATION).param("language", language).param("scriptType", type))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contentId").value(id))
                .andExpect(jsonPath("$.locationId").value(LOCATION))
                .andExpect(jsonPath("$.languageCode").value(language)).andExpect(jsonPath("$.scriptType").value(type))
                .andExpect(jsonPath("$.scriptText").value(text));
        if (suffix == 1) result.andExpect(jsonPath("$.audioUrl").value("/demo-audio/museum.wav"));
        else result.andExpect(jsonPath("$.audioUrl").isEmpty());
    }

    @Test void defaultIsViFullAndReadDoesNotRecordPlayback() throws Exception {
        Long before = jdbc.queryForObject("SELECT count(*) FROM analytics_events", Long.class);
        mvc.perform(get("/api/locations/{id}/guide", LOCATION)).andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("VI")).andExpect(jsonPath("$.scriptType").value("FULL"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analytics_events", Long.class)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FULL", "SHORT"})
    void selectedThenEnThenViPreservesType(String type) throws Exception {
        jdbc.update("INSERT INTO languages(code,display_name) VALUES ('JA','Japanese') ON CONFLICT DO NOTHING");
        UUID selected = insert("JA", type, "APPROVED", "Selected text", 1);
        expectLanguage(type, "JA");
        jdbc.update("UPDATE guide_content_revisions SET status='DRAFT' WHERE id=?", selected);
        expectLanguage(type, "EN");
        jdbc.update("UPDATE guide_content_revisions SET status='PENDING_REVIEW' WHERE language_code='EN'");
        expectLanguage(type, "VI");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_REVIEW", "REJECTED", "ARCHIVED"})
    void nonApprovedRevisionsAreNeverExposed(String state) throws Exception {
        UUID hidden = insert("VI", "FULL", state, "Hidden revision", 2);
        mvc.perform(get("/api/locations/{id}/guide", LOCATION)).andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value("30000000-0000-0000-0000-000000000004"));
        assertThat(guides.findPublicById(hidden)).isEmpty();
        jdbc.update("UPDATE guide_content_revisions SET status=? WHERE location_id=?::uuid", state, LOCATION);
        expectUnavailable("FULL");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n\r "})
    void emptyApprovedTextFallsBackAndUltimatelyReturns404(String text) throws Exception {
        jdbc.update("UPDATE guide_content_revisions SET script_text=? WHERE language_code='EN'", text);
        expectLanguage("FULL", "VI");
        jdbc.update("UPDATE guide_content_revisions SET script_text=? WHERE language_code='VI'", text);
        expectUnavailable("FULL");
    }

    @Test void missingShortNeverFallsBackToFull() throws Exception {
        jdbc.update("UPDATE guide_content_revisions SET status='DRAFT' WHERE script_type='SHORT' AND location_id=?::uuid", LOCATION);
        expectUnavailable("SHORT");
        mvc.perform(get("/api/locations/{id}/guide", LOCATION)).andExpect(status().isOk());
    }

    @Test void latestApprovedRevisionDoesNotBorrowAudioFromOlderRevision() throws Exception {
        UUID latest = insert("VI", "SHORT", "APPROVED", "New approved text", 2);
        insert("VI", "SHORT", "PENDING_REVIEW", "Unpublished replacement", 3);
        mvc.perform(get("/api/locations/{id}/guide", LOCATION).param("scriptType", "SHORT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contentId").value(latest.toString()))
                .andExpect(jsonPath("$.scriptText").value("New approved text"))
                .andExpect(jsonPath("$.audioUrl").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_REVIEW", "REJECTED", "ARCHIVED"})
    void nonPublicLocationIsUnavailableEvenWithApprovedText(String state) throws Exception {
        jdbc.update("UPDATE locations SET status=? WHERE id=?::uuid", state, LOCATION);
        expectUnavailable("FULL");
    }

    @Test void inactiveCategoryOrLanguageCannotPublishText() throws Exception {
        jdbc.update("UPDATE languages SET is_active=false WHERE code='EN'");
        expectLanguage("SHORT", "VI");
        jdbc.update("UPDATE categories SET is_active=false WHERE id=(SELECT category_id FROM locations WHERE id=?::uuid)", LOCATION);
        expectUnavailable("SHORT");
    }

    @Test void unknownLocationReturns404() throws Exception {
        mvc.perform(get("/api/locations/{id}/guide", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private void expectLanguage(String type, String effective) throws Exception {
        mvc.perform(get("/api/locations/{id}/guide", LOCATION).param("language", "JA").param("scriptType", type))
                .andExpect(status().isOk()).andExpect(jsonPath("$.languageCode").value(effective))
                .andExpect(jsonPath("$.scriptType").value(type));
    }

    private void expectUnavailable(String type) throws Exception {
        mvc.perform(get("/api/locations/{id}/guide", LOCATION).param("language", "JA").param("scriptType", type))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Nội dung cho địa điểm này hiện đang được cập nhật."))
                .andExpect(jsonPath("$.scriptText").doesNotExist()).andExpect(jsonPath("$.audioUrl").doesNotExist());
    }

    private UUID insert(String language, String type, String state, String text, int revision) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,status,script_text,revision_no)
                VALUES (?,?::uuid,?,?,?,?,?)
                """, id, LOCATION, language, type, state, text, revision);
        return id;
    }
}
