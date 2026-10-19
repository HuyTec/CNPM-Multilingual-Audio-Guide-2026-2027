package com.application.controller;

import com.application.mapper.DiscoveryMapper;
import com.application.model.Category;
import com.application.model.Location;
import com.application.repository.CategoryRepository;
import com.application.repository.LocationRepository;
import com.application.service.LocationDiscoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@Import({LocationDiscoveryService.class, DiscoveryMapper.class, ApiExceptionHandler.class})
class LocationControllerTest {
    private static final UUID CATEGORY = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID LOCATION = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private LocationRepository locations;
    @MockitoBean
    private CategoryRepository categories;

    @Test
    void searchTrimsKeywordAndReturnsSelectableLocationWithoutGps() throws Exception {
        when(locations.count("Bảo tàng", null)).thenReturn(1L);
        when(locations.search("Bảo tàng", null, 0, 20)).thenReturn(List.of(location()));
        mvc.perform(get("/api/locations").param("keyword", "  Bảo tàng  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].locationId").value(LOCATION.toString()))
                .andExpect(jsonPath("$.items[0].categoryId").value(CATEGORY.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(locations).search("Bảo tàng", null, 0, 20);
    }

    @Test
    void emptyKeywordReturnsSuggestionsAndRetryMessage() throws Exception {
        when(categories.findActive()).thenReturn(List.of(new Category(CATEGORY, "Bảo tàng", 2)));
        mvc.perform(get("/api/locations").param("keyword", "không khớp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.message").value(containsString("từ khóa khác")))
                .andExpect(jsonPath("$.suggestedCategories[0].categoryId").value(CATEGORY.toString()));
    }

    @Test
    void emptyCategorySuggestsOnlyOtherCategoriesWithPublicLocations() throws Exception {
        UUID empty = UUID.randomUUID();
        when(categories.findActiveById(empty)).thenReturn(Optional.of(new Category(empty, "Trống", 0)));
        when(categories.findActive()).thenReturn(List.of(new Category(empty, "Trống", 0),
                new Category(CATEGORY, "Bảo tàng", 2), new Category(UUID.randomUUID(), "Cũng trống", 0)));
        mvc.perform(get("/api/locations").param("categoryId", empty.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.suggestedCategories.length()").value(1));
    }

    @Test
    void unknownCategoryReturns404() throws Exception {
        mvc.perform(get("/api/locations").param("categoryId", CATEGORY.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy danh mục."));
    }

    @Test
    void selectionLoadsTheSameLocationId() throws Exception {
        when(locations.findPublicById(LOCATION)).thenReturn(Optional.of(location()));
        mvc.perform(get("/api/locations/{id}", LOCATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(LOCATION.toString()));
    }

    @Test
    void unknownOrUnpublishedLocationReturns404() throws Exception {
        mvc.perform(get("/api/locations/{id}", LOCATION)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,51", "page,abc", "size,abc", "categoryId,abc"})
    void invalidQueryParametersReturn400(String name, String value) throws Exception {
        mvc.perform(get("/api/locations").param(name, value)).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDetailIdReturns400() throws Exception {
        mvc.perform(get("/api/locations/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void excessiveKeywordReturns400() throws Exception {
        mvc.perform(get("/api/locations").param("keyword", "a".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outOfRangePageDoesNotPretendThereAreNoMatchingLocations() throws Exception {
        when(locations.count("", null)).thenReturn(3L);
        mvc.perform(get("/api/locations").param("page", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.message").value(containsString("trang trước")))
                .andExpect(jsonPath("$.suggestedCategories").isEmpty());
    }

    @Test
    void databaseFailureReturns503WithoutSqlDetails() throws Exception {
        when(locations.count("", null)).thenThrow(new DataAccessResourceFailureException("private SQL details"));
        mvc.perform(get("/api/locations"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("thử lại")))
                .andExpect(content().string(not(containsString("private SQL"))));
    }

    private Location location() {
        return new Location(LOCATION, "Bảo tàng Demo", CATEGORY, "Bảo tàng", 10.77, 106.7, 100);
    }
}
