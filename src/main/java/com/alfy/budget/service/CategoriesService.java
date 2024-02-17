package com.alfy.budget.service;

import com.alfy.budget.model.Category;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class CategoriesService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CategoriesService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Category> getCategories() {
        String query = "SELECT * FROM category";
        HashMap<String, Object> paramMap = new HashMap<>();
        return namedParameterJdbcTemplate.query(query, paramMap, CategoriesService::mapCategory);
    }

    public boolean addCategory(String name) {
        String query = "INSERT INTO category (name)" +
                "VALUES (:name)";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        int rowsAdded = namedParameterJdbcTemplate.update(query, new MapSqlParameterSource(paramMap));
        return rowsAdded == 1;
    }

    public void updateCategory(Category category) {
        String query = "UPDATE category" +
                " SET name=:name" +
                " WHERE id=:id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", category.id);
        paramMap.put("name", category.name);
        namedParameterJdbcTemplate.update(query, new MapSqlParameterSource(paramMap));
    }

    public void deleteCategory(int id) {
        String query = "DELETE FROM category" +
                " WHERE id=:id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", id);
        namedParameterJdbcTemplate.update(query, new MapSqlParameterSource(paramMap));
    }

    private static Category mapCategory(ResultSet resultSet, int rowNum) throws SQLException {
        Category category = new Category();
        category.id = resultSet.getInt("id");
        category.name = resultSet.getString("name");
        return category;
    }

}
