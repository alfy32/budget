package com.alfy.budget.service;

import com.alfy.budget.model.Budget;
import com.alfy.budget.tools.Tools;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
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

    public Budget add(Budget budget) {
        if (budget == null || Strings.isBlank(budget.name) || Tools.isLessThanZero(budget.amount)) {
            return null;
        }

        budget.id = UUID.randomUUID();

        String query = "INSERT INTO budgets (id, name, amount)" +
                "VALUES (:id, :name, :amount)";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", budget.id)
                .addValue("name", budget.name, Types.VARCHAR)
                .addValue("amount", Tools.toDatabaseInt(budget.amount), Types.INTEGER);

        if (namedParameterJdbcTemplate.update(query, sqlParameterSource) != 0) {
            return budget;
        }

        return null;
    }

    public void delete(UUID id) {
        String query = "DELETE FROM budgets WHERE id=:id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void setName(UUID id, String name) {
        String query = "UPDATE budgets"
                + " SET name = :name"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", name, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void setMonthly(UUID id, boolean monthly) {
        String query = "UPDATE budgets"
                + " SET monthly = :monthly"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("monthly", monthly, Types.BOOLEAN);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void setAmount(UUID id, BigDecimal amount) {
        String query = "UPDATE budgets"
                + " SET amount = :amount"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("amount", Tools.toDatabaseInt(amount), Types.INTEGER);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void setTransferAccount(UUID id, String transferAccount) {
        String query = "UPDATE budgets"
                + " SET transfer_account = :transfer_account"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("transfer_account", transferAccount, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    private static Budget map(ResultSet resultSet, int rowNum) throws SQLException {
        Budget budget = new Budget();
        budget.id = UUID.fromString(resultSet.getString("id"));
        budget.name = resultSet.getString("name");
        budget.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));
        budget.monthly = resultSet.getBoolean("monthly");
        budget.transferAccount = resultSet.getString("transfer_account");
        return budget;
    }
}
