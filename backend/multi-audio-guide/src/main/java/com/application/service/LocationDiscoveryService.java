package com.application.service;

import com.application.dto.CategoryResponse;
import com.application.dto.LocationResponse;
import com.application.dto.LocationSearchResponse;
import com.application.mapper.DiscoveryMapper;
import com.application.repository.CategoryRepository;
import com.application.repository.LocationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LocationDiscoveryService {
    private final LocationRepository locations;
    private final CategoryRepository categories;
    private final DiscoveryMapper mapper;

    public LocationDiscoveryService(LocationRepository locations, CategoryRepository categories, DiscoveryMapper mapper) {
        this.locations = locations;
        this.categories = categories;
        this.mapper = mapper;
    }

    /** Apply manual-search rules; an empty result is a valid response, not an API error. */
    public LocationSearchResponse search(String keyword, UUID categoryId, int page, int size) {
        String normalized = keyword == null ? "" : keyword.strip();
        if (normalized.length() > 255 || page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Từ khóa tối đa 255 ký tự; page từ 0 và size từ 1 đến 50.");
        }
        if (categoryId != null && categories.findActiveById(categoryId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục.");
        }

        long total = locations.count(normalized, categoryId);
        List<LocationResponse> items = locations.search(normalized, categoryId, page, size)
                .stream().map(mapper::toResponse).toList();
        String message = "";
        List<CategoryResponse> suggestions = List.of();
        if (total == 0) {
            message = !normalized.isEmpty()
                    ? "Không tìm thấy địa điểm phù hợp. Hãy thử từ khóa khác hoặc danh mục khác."
                    : "Chưa có địa điểm trong danh mục hoặc danh sách này. Hãy thử danh mục khác.";
            suggestions = categories.findActive().stream()
                    .filter(category -> category.locationCount() > 0 && !category.id().equals(categoryId))
                    .map(mapper::toResponse).toList();
        } else if (items.isEmpty()) {
            message = "Trang này không có kết quả. Hãy quay lại trang trước.";
        }
        long totalPages = total / size + (total % size == 0 ? 0 : 1);
        return new LocationSearchResponse(items, page, size, total, totalPages, message, suggestions);
    }

    public List<CategoryResponse> categories() {
        return categories.findActive().stream().map(mapper::toResponse).toList();
    }

    public LocationResponse detail(UUID locationId) {
        return locations.findPublicById(locationId).map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa điểm."));
    }
}
