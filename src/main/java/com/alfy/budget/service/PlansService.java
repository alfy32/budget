package com.alfy.budget.service;

import com.alfy.budget.model.Plan;
import com.alfy.budget.model.PlanGroup;
import com.alfy.budget.model.PlanIncomeItem;
import com.alfy.budget.model.PlanLine;
import com.alfy.budget.tools.Tools;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Budget plans. A plan is in effect from its start month until the next plan starts; the plan in effect
 * today is the current one (a newer plan can be set up ahead of time). A plan without a start month is a
 * proposal: it's ignored everywhere until it's committed to a month.
 *
 * categories.budget_id and budgets.amount mirror the current plan, so the rest of the app (category
 * pages, transfers) can keep asking "which budget is this category in?". syncCurrentPlan keeps them in step.
 */
public class PlansService {

    private final NamedParameterJdbcTemplate jdbc;

    public PlansService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- Plans -------------------------------------------------------------------------------

    /** Committed plans, oldest first, each with its end date filled in. */
    public List<Plan> list() {
        List<Plan> plans = jdbc.query("SELECT * FROM plans WHERE start_month IS NOT NULL ORDER BY start_month", PlansService::map);
        for (int i = 0; i < plans.size() - 1; i++) {
            plans.get(i).end = plans.get(i + 1).startMonth.minusDays(1);
        }
        return plans;
    }

    /** Proposed plans (no start month yet). */
    public List<Plan> proposals() {
        return jdbc.query("SELECT * FROM plans WHERE start_month IS NULL ORDER BY rowid", PlansService::map);
    }

    public Plan get(UUID id) {
        return list().stream().filter(plan -> plan.id.equals(id)).findFirst()
                .orElseGet(() -> proposals().stream().filter(plan -> plan.id.equals(id)).findFirst()
                        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "No such plan (it may have been deleted)")));
    }

    /** The plan in effect for the month containing date. Months before the first plan use the first plan. */
    public Plan forMonth(LocalDate date) {
        LocalDate month = date.withDayOfMonth(1);
        List<Plan> plans = list();
        Plan found = plans.isEmpty() ? null : plans.getFirst();
        for (Plan plan : plans) {
            if (!plan.startMonth.isAfter(month)) {
                found = plan;
            }
        }
        return found;
    }

    public Plan current() {
        return forMonth(LocalDate.now());
    }

    /** The plan before this one, or null. For a proposal, that's the current plan. */
    public Plan previous(Plan plan) {
        if (plan.isProposed()) {
            return current();
        }
        Plan previous = null;
        for (Plan other : list()) {
            if (other.startMonth.isBefore(plan.startMonth)) {
                previous = other;
            }
        }
        return previous;
    }

    /** A copy of a plan (lines, income items, pay and target) starting at another month, or a proposal if startMonth is null. */
    public UUID copy(UUID fromPlanId, LocalDate startMonth) {
        UUID id = UUID.randomUUID();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", Db.id(id))
                .addValue("from", Db.id(fromPlanId))
                .addValue("start", startMonth == null ? null : Db.date(startMonth.withDayOfMonth(1)));

        jdbc.update("""
                INSERT INTO plans (id, start_month, take_home, savings_target)
                SELECT :id, :start, take_home, savings_target FROM plans WHERE id = :from
                """, params);
        jdbc.update("""
                INSERT INTO plan_lines (plan_id, category_id, budget_id, amount)
                SELECT :id, category_id, budget_id, amount FROM plan_lines WHERE plan_id = :from
                """, params);
        jdbc.update("""
                INSERT INTO plan_income_items (id, plan_id, name, amount)
                SELECT lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' ||
                             hex(randomblob(2)) || '-' || hex(randomblob(6))),
                       :id, name, amount
                FROM plan_income_items WHERE plan_id = :from
                """, params);

        syncCurrentPlan();
        return id;
    }

    public boolean startMonthTaken(LocalDate startMonth, UUID exceptPlanId) {
        return startMonth != null
                && list().stream().anyMatch(plan -> plan.startMonth.equals(startMonth.withDayOfMonth(1)) && !plan.id.equals(exceptPlanId));
    }

    public void update(UUID planId, LocalDate startMonth, BigDecimal takeHome, BigDecimal savingsTarget, String notes) {
        jdbc.update("""
                        UPDATE plans
                        SET start_month = :start, take_home = :takeHome, savings_target = :target, notes = :notes
                        WHERE id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("id", Db.id(planId))
                        .addValue("start", startMonth == null ? null : Db.date(startMonth.withDayOfMonth(1)))
                        .addValue("takeHome", Tools.toDatabaseInt(takeHome))
                        .addValue("target", savingsTarget == null ? null : Tools.toDatabaseInt(savingsTarget))
                        .addValue("notes", notes));
        syncCurrentPlan();
    }

    /** The only committed plan can't be deleted (proposals always can). */
    public boolean delete(UUID planId) {
        Plan plan = get(planId);
        if (!plan.isProposed() && list().size() <= 1) {
            return false;
        }
        jdbc.update("DELETE FROM plans WHERE id = :id", new MapSqlParameterSource("id", Db.id(planId)));
        syncCurrentPlan();
        return true;
    }

    // ---- Lines -------------------------------------------------------------------------------

    public List<PlanLine> lines(UUID planId) {
        return jdbc.query("""
                        SELECT plan_lines.*, categories.name AS category_name, budgets.name AS budget_name, budgets.plan_group
                        FROM plan_lines
                        JOIN categories ON categories.id = plan_lines.category_id
                        JOIN budgets ON budgets.id = plan_lines.budget_id
                        WHERE plan_id = :planId
                        ORDER BY lower(budgets.name), lower(categories.name)
                        """,
                new MapSqlParameterSource("planId", Db.id(planId)), PlansService::mapLine);
    }

    /** Sets the yearly amount of lines already in the plan (by category id). */
    public void setAmounts(UUID planId, Map<UUID, BigDecimal> amountsByCategory) {
        for (Map.Entry<UUID, BigDecimal> entry : amountsByCategory.entrySet()) {
            jdbc.update("UPDATE plan_lines SET amount = :amount WHERE plan_id = :planId AND category_id = :categoryId",
                    new MapSqlParameterSource()
                            .addValue("planId", Db.id(planId))
                            .addValue("categoryId", Db.id(entry.getKey()))
                            .addValue("amount", Tools.toDatabaseInt(entry.getValue() == null ? BigDecimal.ZERO : entry.getValue().abs())));
        }
        syncCurrentPlan();
    }

    /** Adds a category to the plan under a budget, or moves it there if it's already in the plan. */
    public void putLine(UUID planId, UUID categoryId, UUID budgetId) {
        jdbc.update("""
                        INSERT INTO plan_lines (plan_id, category_id, budget_id, amount)
                        VALUES (:planId, :categoryId, :budgetId, 0)
                        ON CONFLICT (plan_id, category_id) DO UPDATE SET budget_id = excluded.budget_id
                        """,
                new MapSqlParameterSource()
                        .addValue("planId", Db.id(planId))
                        .addValue("categoryId", Db.id(categoryId))
                        .addValue("budgetId", Db.id(budgetId)));
        syncCurrentPlan();
    }

    public void removeLine(UUID planId, UUID categoryId) {
        jdbc.update("DELETE FROM plan_lines WHERE plan_id = :planId AND category_id = :categoryId",
                new MapSqlParameterSource()
                        .addValue("planId", Db.id(planId))
                        .addValue("categoryId", Db.id(categoryId)));
        syncCurrentPlan();
    }

    /** Puts a category under a budget in the current plan (or takes it out when budgetId is null). */
    public void setBudgetInCurrentPlan(UUID categoryId, UUID budgetId) {
        Plan current = current();
        if (current == null) {
            return;
        }
        if (budgetId == null) {
            removeLine(current.id, categoryId);
        } else {
            putLine(current.id, categoryId, budgetId);
        }
    }

    // ---- From income -------------------------------------------------------------------------

    public List<PlanIncomeItem> incomeItems(UUID planId) {
        return jdbc.query("SELECT * FROM plan_income_items WHERE plan_id = :planId ORDER BY lower(name)",
                new MapSqlParameterSource("planId", Db.id(planId)), (resultSet, rowNum) -> {
                    PlanIncomeItem item = new PlanIncomeItem();
                    item.id = Db.getId(resultSet, "id");
                    item.name = resultSet.getString("name");
                    item.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));
                    return item;
                });
    }

    public void addIncomeItem(UUID planId, String name, BigDecimal amount) {
        if (name == null || name.isBlank()) {
            return;
        }
        jdbc.update("INSERT INTO plan_income_items (id, plan_id, name, amount) VALUES (:id, :planId, :name, :amount)",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(UUID.randomUUID()))
                        .addValue("planId", Db.id(planId))
                        .addValue("name", name.strip())
                        .addValue("amount", Tools.toDatabaseInt(amount == null ? BigDecimal.ZERO : amount.abs())));
    }

    public void setIncomeItemAmounts(UUID planId, Map<UUID, BigDecimal> amountsById) {
        for (Map.Entry<UUID, BigDecimal> entry : amountsById.entrySet()) {
            jdbc.update("UPDATE plan_income_items SET amount = :amount WHERE plan_id = :planId AND id = :id",
                    new MapSqlParameterSource()
                            .addValue("planId", Db.id(planId))
                            .addValue("id", Db.id(entry.getKey()))
                            .addValue("amount", Tools.toDatabaseInt(entry.getValue() == null ? BigDecimal.ZERO : entry.getValue().abs())));
        }
    }

    public void removeIncomeItem(UUID planId, UUID itemId) {
        jdbc.update("DELETE FROM plan_income_items WHERE plan_id = :planId AND id = :id",
                new MapSqlParameterSource()
                        .addValue("planId", Db.id(planId))
                        .addValue("id", Db.id(itemId)));
    }

    // ---- Totals ------------------------------------------------------------------------------

    /**
     * Per-month totals for a plan, the way the planning sheet adds them up.
     * savings = takeHome - fromIncome - spending
     */
    public record Totals(BigDecimal takeHome, BigDecimal fromIncome, BigDecimal spending, BigDecimal monthly,
                         BigDecimal yearly, BigDecimal other, BigDecimal savings) {
    }

    public Totals totals(Plan plan) {
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal yearly = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        for (PlanLine line : lines(plan.id)) {
            switch (line.group) {
                case MONTHLY -> monthly = monthly.add(line.amount);
                case YEARLY -> yearly = yearly.add(line.amount);
                case INCOME -> income = income.add(line.amount);
                case OTHER -> other = other.add(line.amount);
            }
        }
        for (PlanIncomeItem item : incomeItems(plan.id)) {
            income = income.add(item.amount);
        }

        BigDecimal twelve = BigDecimal.valueOf(12);
        BigDecimal monthlyPerMonth = monthly.divide(twelve, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal yearlyPerMonth = yearly.divide(twelve, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal incomePerMonth = income.divide(twelve, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal spending = monthlyPerMonth.add(yearlyPerMonth);
        return new Totals(plan.takeHome, incomePerMonth, spending, monthlyPerMonth, yearlyPerMonth,
                other.divide(twelve, 2, java.math.RoundingMode.HALF_UP),
                plan.takeHome.subtract(incomePerMonth).subtract(spending));
    }

    // ---- Keeping the rest of the app in step -------------------------------------------------

    /**
     * Makes categories.budget_id and budgets.amount match the current plan. Called after any plan change and
     * when the app starts (a plan set up ahead of time becomes current when its month comes).
     */
    public void syncCurrentPlan() {
        Plan current = current();
        if (current == null) {
            return;
        }
        MapSqlParameterSource params = new MapSqlParameterSource("planId", Db.id(current.id));
        jdbc.update("""
                UPDATE categories
                SET budget_id = (SELECT budget_id FROM plan_lines WHERE plan_id = :planId AND category_id = categories.id)
                """, params);
        jdbc.update("""
                UPDATE budgets
                SET amount = coalesce((SELECT CAST(round(sum(amount) / 12.0) AS INTEGER) FROM plan_lines WHERE plan_id = :planId AND budget_id = budgets.id), 0)
                """, params);
    }

    private static Plan map(ResultSet resultSet, int rowNum) throws SQLException {
        Plan plan = new Plan();
        plan.id = Db.getId(resultSet, "id");
        plan.startMonth = Db.getDate(resultSet, "start_month");
        plan.takeHome = Tools.fromDatabaseInt(resultSet.getInt("take_home"));
        int target = resultSet.getInt("savings_target");
        plan.savingsTarget = resultSet.wasNull() ? null : Tools.fromDatabaseInt(target);
        plan.notes = resultSet.getString("notes");
        return plan;
    }

    private static PlanLine mapLine(ResultSet resultSet, int rowNum) throws SQLException {
        PlanLine line = new PlanLine();
        line.planId = Db.getId(resultSet, "plan_id");
        line.categoryId = Db.getId(resultSet, "category_id");
        line.categoryName = resultSet.getString("category_name");
        line.budgetId = Db.getId(resultSet, "budget_id");
        line.budgetName = resultSet.getString("budget_name");
        line.group = PlanGroup.fromDb(resultSet.getString("plan_group"));
        line.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));
        return line;
    }

}
