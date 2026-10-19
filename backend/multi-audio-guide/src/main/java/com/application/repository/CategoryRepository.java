package com.application.repository;

import com.application.model.Category;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CategoryRepository {
    private static final String SELECT_ACTIVE = """
            SELECT c.id, c.name, count(l.id) AS location_count
            FROM categories c LEFT JOIN locations l ON l.category_id = c.id AND l.status = 'APPROVED'
            WHERE c.is_active = TRUE
            """;
    private static final RowMapper<Category> ROW_MAPPER = (rs, rowNum) -> new Category(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getLong("location_count"));

    private final NamedParameterJdbcTemplate jdbc;

    public CategoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Category> findActive() {
        return jdbc.query(SELECT_ACTIVE + " GROUP BY c.id, c.name ORDER BY lower(c.name), c.id", ROW_MAPPER);
    }

    public Optional<Category> findActiveById(UUID categoryId) {
        return jdbc.query(SELECT_ACTIVE + " AND c.id = :id GROUP BY c.id, c.name",
                Map.of("id", categoryId), ROW_MAPPER).stream().findFirst();
    }
}
