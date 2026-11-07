package com.alfy.budget.service;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.PlanGroup;
import com.alfy.budget.tools.Tools;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BudgetsService {

    private final NamedParameterJdbcTemplate jdbc;

    public BudgetsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Budget> list() {
        return jdbc.query("SELECT * FROM budgets ORDER BY name", BudgetsService::map);
    }

    public Map<UUID, Budget> listBudgetsById() {
        Map<UUID, Budget> budgetsById = new HashMap<>();
        for (Budget budget : list()) {
            budgetsById.put(budget.id, budget);
        }
        return budgetsById;
    }

    public Budget get(UUID id) {
        return jdbc.queryForObject("SELECT * FROM budgets WHERE id = :id",
                new MapSqlParameterSource("id", Db.id(id)), BudgetsService::map);
    }

    public Budget add(Budget budget) {
        if (budget == null || budget.name == null || budget.name.isBlank() || Tools.isLessThanZero(budget.amount)) {
            return null;
        }

        budget.id = UUID.randomUUID();

        jdbc.update("INSERT INTO budgets (id, name, amount) VALUES (:id, :name, :amount)",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(budget.id))
                        .addValue("name", budget.name)
                        .addValue("amount", Tools.toDatabaseInt(budget.amount)));

        return budget;
    }

    /** How many plans have a line in this budget. */
    public int plansUsing(UUID id) {
        Integer count = jdbc.queryForObject("SELECT count(DISTINCT plan_id) FROM plan_lines WHERE budget_id = :id",
                new MapSqlParameterSource("id", Db.id(id)), Integer.class);
        return count == null ? 0 : count;
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM budgets WHERE id = :id", new MapSqlParameterSource("id", Db.id(id)));
    }

    public void setName(UUID id, String name) {
        update(id, "name", name);
    }

    public void setMonthly(UUID id, boolean monthly) {
        update(id, "monthly", monthly ? 1 : 0);
    }

    public void setGroup(UUID id, PlanGroup group) {
        update(id, "plan_group", (group == null ? PlanGroup.MONTHLY : group).dbValue);
    }

    public void setTransferAccount(UUID id, String transferAccount) {
        update(id, "transfer_account", transferAccount == null || transferAccount.isBlank() ? null : transferAccount);
    }

    private void update(UUID id, String column, Object value) {
        jdbc.update("UPDATE budgets SET " + column + " = :value WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(id))
                        .addValue("value", value));
    }

    private static Budget map(ResultSet resultSet, int rowNum) throws SQLException {
        Budget budget = new Budget();
        budget.id = Db.getId(resultSet, "id");
        budget.name = resultSet.getString("name");
        budget.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));
        budget.monthly = resultSet.getBoolean("monthly");
        budget.transferAccount = resultSet.getString("transfer_account");
        budget.group = PlanGroup.fromDb(resultSet.getString("plan_group"));
        return budget;
    }
}
