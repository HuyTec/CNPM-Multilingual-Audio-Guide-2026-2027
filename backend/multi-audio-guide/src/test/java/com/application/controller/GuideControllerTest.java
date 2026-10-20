package com.application.controller;

import com.application.model.GuideContent;
import com.application.repository.GuideRepository;
import com.application.repository.PlaybackRepository;
import com.application.service.PlaybackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP/service checks only; SQL selection is covered by GuideIntegrationTest. */
@WebMvcTest(GuideController.class)
@Import({PlaybackService.class, ApiExceptionHandler.class})
class GuideControllerTest {
    private static final UUID LOCATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    @Autowired MockMvc mvc;
    @MockitoBean GuideRepository guides;
    @MockitoBean PlaybackRepository playbacks;

    @Test void defaultProfileReturnsFullTextWithoutAudioOrPlayback() throws Exception {
        GuideContent guide = new GuideContent(UUID.randomUUID(), LOCATION, "Demo", "VI", "FULL", "Bài đầy đủ", null);
        when(guides.find(LOCATION, "VI", "FULL")).thenReturn(Optional.of(guide));
        mvc.perform(get("/api/locations/{id}/guide", LOCATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(guide.contentId().toString()))
                .andExpect(jsonPath("$.locationId").value(LOCATION.toString()))
                .andExpect(jsonPath("$.languageCode").value("VI"))
                .andExpect(jsonPath("$.scriptType").value("FULL"))
                .andExpect(jsonPath("$.scriptText").value("Bài đầy đủ"))
                .andExpect(jsonPath("$.audioUrl").isEmpty());
        verifyNoInteractions(playbacks);
    }

    @Test void normalizedLanguageAndShortModeArePassedToSelection() throws Exception {
        when(guides.find(LOCATION, "EN", "SHORT")).thenReturn(Optional.of(
                new GuideContent(UUID.randomUUID(), LOCATION, "Demo", "EN", "SHORT", "Short guide", null)));
        mvc.perform(get("/api/locations/{id}/guide", LOCATION).param("language", " en ").param("scriptType", "SHORT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.languageCode").value("EN"))
                .andExpect(jsonPath("$.scriptType").value("SHORT"));
        verify(guides).find(LOCATION, "EN", "SHORT");
    }

    @ParameterizedTest
    @CsvSource({"language,?", "language,EN-US-extra", "scriptType,summary", "scriptType,short"})
    void invalidSelectionReturns400BeforeQuery(String name, String value) throws Exception {
        mvc.perform(get("/api/locations/{id}/guide", LOCATION).param(name, value)).andExpect(status().isBadRequest());
        verifyNoInteractions(guides, playbacks);
    }

    @Test void invalidLocationIdReturns400() throws Exception {
        mvc.perform(get("/api/locations/not-a-uuid/guide")).andExpect(status().isBadRequest());
        verifyNoInteractions(guides, playbacks);
    }

    @Test void noApprovedContentReturnsProblemDetailWithoutAPlayableContent() throws Exception {
        mvc.perform(get("/api/locations/{id}/guide", LOCATION))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Nội dung cho địa điểm này hiện đang được cập nhật."))
                .andExpect(jsonPath("$.scriptText").doesNotExist())
                .andExpect(jsonPath("$.audioUrl").doesNotExist());
        verifyNoInteractions(playbacks);
    }

    @Test void databaseFailureIs503RatherThanNoApprovedContent() throws Exception {
        when(guides.find(LOCATION, "VI", "FULL")).thenThrow(new DataAccessResourceFailureException("private SQL"));
        mvc.perform(get("/api/locations/{id}/guide", LOCATION))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("thử lại")))
                .andExpect(content().string(not(containsString("private SQL"))));
    }
}
