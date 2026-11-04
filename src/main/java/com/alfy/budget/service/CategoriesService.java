package com.alfy.budget.service;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CategoriesService {

    private final NamedParameterJdbcTemplate jdbc;

    public CategoriesService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Category> list() {
        return jdbc.query("SELECT * FROM categories ORDER BY name", CategoriesService::map);
    }

    public Map<UUID, Category> getCategoriesById() {
        Map<UUID, Category> categoriesById = new HashMap<>();
        for (Category category : list()) {
            categoriesById.put(category.id, category);
        }
        return categoriesById;
    }

    public Category get(UUID id) {
        return jdbc.queryForObject("SELECT * FROM categories WHERE id = :id",
                new MapSqlParameterSource("id", Db.id(id)), CategoriesService::map);
    }

    public void add(Category category) {
        if (category == null || category.name == null || category.name.isBlank()) {
            return;
        }

        category.id = UUID.randomUUID();

        jdbc.update("INSERT INTO categories (id, name, budget_id) VALUES (:id, :name, :budgetId)",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(category.id))
                        .addValue("name", category.name)
                        .addValue("budgetId", category.budget == null ? null : Db.id(category.budget.id)));
    }

    public void setName(UUID id, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        jdbc.update("UPDATE categories SET name = :name WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(id))
                        .addValue("name", name));
    }

    public void setBudget(UUID id, UUID budgetId) {
        jdbc.update("UPDATE categories SET budget_id = :budgetId WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(id))
                        .addValue("budgetId", Db.id(budgetId)));
    }

    public void setAccount(UUID id, UUID accountId) {
        jdbc.update("UPDATE categories SET account_id = :accountId WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(id))
                        .addValue("accountId", Db.id(accountId)));
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM categories WHERE id = :id", new MapSqlParameterSource("id", Db.id(id)));
    }

    private static Category map(ResultSet resultSet, int rowNum) throws SQLException {
        Category category = new Category();
        category.id = Db.getId(resultSet, "id");
        category.name = resultSet.getString("name");

        UUID budgetId = Db.getId(resultSet, "budget_id");
        if (budgetId != null) {
            category.budget = new Budget();
            category.budget.id = budgetId;
        }

        category.accountId = Db.getId(resultSet, "account_id");
        return category;
    }
}
