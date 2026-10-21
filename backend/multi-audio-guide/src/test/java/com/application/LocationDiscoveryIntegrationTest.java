package com.application;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end HTTP-layer tests against a dedicated PostgreSQL/PostGIS database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@Transactional
@org.springframework.test.context.jdbc.Sql("/restore-discovery-fixtures.sql")
class LocationDiscoveryIntegrationTest {
    private static final UUID CATEGORY = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String LOCATION = "20000000-0000-0000-0000-000000000001";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Flyway flyway;

    @Test
    void migrationsValidateAndDoNotRunTwice() {
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4.1");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void keywordMatchingIsCaseInsensitiveAndRanksExactMatchFirst() throws Exception {
        mvc.perform(get("/api/locations").param("keyword", "BẢO TÀNG DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].locationId").value(LOCATION));
    }

    @Test
    void prefixMatchRanksBeforeSubstring() throws Exception {
        mvc.perform(get("/api/locations").param("keyword", "Bảo tàng").param("categoryId", CATEGORY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].locationId").value(LOCATION));
    }

    @Test
    void categoryFilterAndKeywordAreCombined() throws Exception {
        mvc.perform(get("/api/locations").param("categoryId", CATEGORY.toString()).param("keyword", "Không gian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].locationId").value("20000000-0000-0000-0000-000000000002"));
    }

    @Test
    void categoryCatalogIncludesEmptyCategoriesAndCountsOnlyApprovedLocations() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.categoryId == '10000000-0000-0000-0000-000000000001')].locationCount").value(contains(2)))
                .andExpect(jsonPath("$[?(@.categoryId == '10000000-0000-0000-0000-000000000003')].locationCount").value(contains(0)));
    }

    @Test
    void emptyCategoryReturnsAlternatives() throws Exception {
        mvc.perform(get("/api/locations").param("categoryId", "10000000-0000-0000-0000-000000000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.suggestedCategories.length()").value(3));
    }

    @Test
    void noMatchingKeywordReturnsEmptyStateMessage() throws Exception {
        mvc.perform(get("/api/locations").param("keyword", "không có tên này"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.message").value(containsString("từ khóa khác")));
    }

    @Test
    void paginationIsStableAndDoesNotRequireGps() throws Exception {
        mvc.perform(get("/api/locations").param("keyword", "Demo").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items[0].locationId").value(LOCATION));
        mvc.perform(get("/api/locations").param("keyword", "Demo").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].locationId").value("20000000-0000-0000-0000-000000000003"));
    }

    @Test
    void detailUsesSelectedIdAndReturnsCoordinatesInCorrectOrder() throws Exception {
        mvc.perform(get("/api/locations/{id}", LOCATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(LOCATION))
                .andExpect(jsonPath("$.latitude").value(10.77))
                .andExpect(jsonPath("$.longitude").value(106.7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_REVIEW", "REJECTED", "ARCHIVED"})
    void nonPublicStatusesAreHiddenFromSearchAndDetail(String state) throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO locations (id, category_id, name, status) VALUES (?, ?, ?, ?)",
                id, CATEGORY, "Hidden state fixture", state);
        mvc.perform(get("/api/locations").param("keyword", "Hidden state fixture"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/locations/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void inactiveCategoryHidesItsLocations() throws Exception {
        mvc.perform(get("/api/locations/20000000-0000-0000-0000-000000000005"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/locations").param("categoryId", "10000000-0000-0000-0000-000000000004"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!", "\\", "' OR 1=1 --"})
    void wildcardAndSqlLikeInputAreTreatedAsLiteralText(String keyword) throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO locations (id, category_id, name, status, coordinates, geofence_radius_m)
                VALUES (?, ?, ?, 'APPROVED', ST_SetSRID(ST_MakePoint(106.7, 10.77), 4326)::geography, 100)
                """, id, CATEGORY, "Literal " + keyword);
        mvc.perform(get("/api/locations").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].locationId").value(id.toString()));
    }
}
