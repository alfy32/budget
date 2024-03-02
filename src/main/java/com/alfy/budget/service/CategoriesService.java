package com.alfy.budget.service;

import com.alfy.budget.model.Category;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CategoriesService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CategoriesService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Category> getCategories() {
        String query = "SELECT id, name FROM categories";
        HashMap<String, Object> paramMap = new HashMap<>();
        return namedParameterJdbcTemplate.query(query, paramMap, CategoriesService::map);
    }

    public Map<UUID, Category> getCategoriesById() {
        HashMap<UUID, Category> categoriesById = new HashMap<>();
        for (Category category : getCategories()) {
            categoriesById.put(category.id, category);
        }
        return categoriesById;
    }

    public Category get(UUID id) {
        String query = "SELECT id, name FROM categories WHERE id=:id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        return namedParameterJdbcTemplate.queryForObject(query, sqlParameterSource, CategoriesService::map);
    }

    public void add(Category category) {
        if (category == null || Strings.isBlank(category.name)) {
            return;
        }

        category.id = UUID.randomUUID();

        String query = "INSERT INTO categories (id, name)" +
                "VALUES (:id, :name)";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", category.id)
                .addValue("name", category.name, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void update(Category category) {
        if (category == null || category.id == null || Strings.isBlank(category.name)) {
            return;
        }

        String query = "UPDATE categories" +
                " SET name=:name" +
                " WHERE id=:id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", category.id)
                .addValue("name", category.name, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void delete(UUID id) {
        String query = "DELETE FROM categories WHERE id=:id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    private static Category map(ResultSet resultSet, int rowNum) throws SQLException {
        Category category = new Category();
        category.id = UUID.fromString(resultSet.getString("id"));
        category.name = resultSet.getString("name");
        return category;
    }
}
