package com.alfy.budget.service;

import com.alfy.budget.model.Budget;
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

public class BudgetsService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public BudgetsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Budget> list() {
        String query = "SELECT * FROM budgets";
        HashMap<String, Object> paramMap = new HashMap<>();
        return namedParameterJdbcTemplate.query(query, paramMap, BudgetsService::map);
    }

    public Map<UUID, Budget> listBudgetsById() {
        HashMap<UUID, Budget> budgetsById = new HashMap<>();

        for (Budget budget : list()) {
            budgetsById.put(budget.id, budget);
        }

        return budgetsById;
    }

    public Budget get(UUID id) {
        String query = "SELECT * FROM budgets WHERE id=:id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        return namedParameterJdbcTemplate.queryForObject(query, sqlParameterSource, BudgetsService::map);
    }

    public void add(Budget budget) {
        if (budget == null || Strings.isBlank(budget.name) || budget.amount <= 0) {
            return;
        }

        budget.id = UUID.randomUUID();

        String query = "INSERT INTO budgets (id, name, amount)" +
                "VALUES (:id, :name, :amount)";

        double amount = budget.amount * 100d;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", budget.id)
                .addValue("name", budget.name, Types.VARCHAR)
                .addValue("amount", (int) amount, Types.INTEGER);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    private static Budget map(ResultSet resultSet, int rowNum) throws SQLException {
        Budget budget = new Budget();
        budget.id = UUID.fromString(resultSet.getString("id"));
        budget.name = resultSet.getString("name");
        budget.amount = resultSet.getDouble("amount") / 100d;
        return budget;
    }
}
