package com.alfy.budget.service;

import com.alfy.budget.model.Plan;
import com.alfy.budget.model.PlanGroup;
import com.alfy.budget.model.PaySection;
import com.alfy.budget.model.PlanPayItem;
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
                INSERT INTO plans (id, start_month, gross_salary, savings_target, hsa_limit)
                SELECT :id, :start, gross_salary, savings_target, hsa_limit FROM plans WHERE id = :from
                """, params);
        jdbc.update("""
                INSERT INTO plan_lines (plan_id, category_id, budget_id, amount)
                SELECT :id, category_id, budget_id, amount FROM plan_lines WHERE plan_id = :from
                """, params);
        jdbc.update("""
                INSERT INTO plan_pay_items (id, plan_id, section, name, kind, base, value, retirement, hsa, sort)
                SELECT lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' ||
                             hex(randomblob(2)) || '-' || hex(randomblob(6))),
                       :id, section, name, kind, base, value, retirement, hsa, sort
                FROM plan_pay_items WHERE plan_id = :from
                """, params);

        syncCurrentPlan();
        return id;
    }

    public boolean startMonthTaken(LocalDate startMonth, UUID exceptPlanId) {
        return startMonth != null
                && list().stream().anyMatch(plan -> plan.startMonth.equals(startMonth.withDayOfMonth(1)) && !plan.id.equals(exceptPlanId));
    }

    public void update(UUID planId, LocalDate startMonth, BigDecimal grossSalary, String notes) {
        jdbc.update("""
                        UPDATE plans
                        SET start_month = :start, gross_salary = :gross, notes = :notes
                        WHERE id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("id", Db.id(planId))
                        .addValue("start", startMonth == null ? null : Db.date(startMonth.withDayOfMonth(1)))
                        .addValue("gross", Tools.toDatabaseInt(grossSalary == null ? BigDecimal.ZERO : grossSalary.abs()))
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

    // ---- Pay ---------------------------------------------------------------------------------

    public List<PlanPayItem> payItems(UUID planId) {
        return jdbc.query("SELECT * FROM plan_pay_items WHERE plan_id = :planId ORDER BY sort, rowid",
                new MapSqlParameterSource("planId", Db.id(planId)), (resultSet, rowNum) -> {
                    PlanPayItem item = new PlanPayItem();
                    item.id = Db.getId(resultSet, "id");
                    item.section = PaySection.fromDb(resultSet.getString("section"));
                    item.name = resultSet.getString("name");
                    item.percent = "percent".equals(resultSet.getString("kind"));
                    item.ofTaxable = "taxable".equals(resultSet.getString("base"));
                    item.value = Tools.fromDatabaseInt(resultSet.getInt("value"));
                    item.retirement = resultSet.getBoolean("retirement");
                    item.hsa = resultSet.getBoolean("hsa");
                    return item;
                });
    }

    /** Saves a pay line's settings (value is dollars per year, or a percent like 6.2). */
    public void updatePayItem(UUID planId, UUID itemId, String name, boolean percent, boolean ofTaxable, BigDecimal value,
                              boolean retirement, boolean hsa) {
        jdbc.update("""
                        UPDATE plan_pay_items
                        SET name = coalesce(:name, name), kind = :kind, base = :base, value = :value, retirement = :retirement, hsa = :hsa
                        WHERE plan_id = :planId AND id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("planId", Db.id(planId))
                        .addValue("id", Db.id(itemId))
                        .addValue("name", name == null || name.isBlank() ? null : name.strip())
                        .addValue("kind", percent ? "percent" : "amount")
                        .addValue("base", ofTaxable ? "taxable" : "gross")
                        .addValue("value", Tools.toDatabaseInt(value == null ? BigDecimal.ZERO : value.abs()))
                        .addValue("retirement", retirement ? 1 : 0)
                        .addValue("hsa", hsa ? 1 : 0));
    }

    public void setHsaLimit(UUID planId, BigDecimal limit) {
        jdbc.update("UPDATE plans SET hsa_limit = :limit WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(planId))
                        .addValue("limit", limit == null ? null : Tools.toDatabaseInt(limit.abs())));
    }

    /**
     * The HSA check: yours + the employer's against the legal limit, and the most you can put in per paycheck while
     * keeping one whole paycheck (yours + the employer's) under the limit, in case payroll puts an extra paycheck
     * in the year. With p paychecks a year, (p + 1) of yours + (p + 1) of the employer's must fit:
     * yours per paycheck <= (limit - employer per year * (p + 1) / p) / (p + 1).
     */
    public record HsaCheck(BigDecimal yours, BigDecimal employer, BigDecimal total, BigDecimal limit, BigDecimal spare,
                           BigDecimal onePaycheck, boolean ok, BigDecimal mostPerPaycheck) {
    }

    public static final int PAYCHECKS_PER_YEAR = 24;

    public HsaCheck hsaCheck(Plan plan) {
        BigDecimal gross = plan.grossSalary;
        BigDecimal yours = BigDecimal.ZERO;
        BigDecimal employer = BigDecimal.ZERO;
        for (PlanPayItem item : payItems(plan.id)) {
            if (item.hsa) {
                BigDecimal amount = item.amount(gross, gross);
                if (item.section == PaySection.PRETAX) {
                    yours = yours.add(amount);
                } else {
                    employer = employer.add(amount);
                }
            }
        }
        BigDecimal total = yours.add(employer);
        BigDecimal paychecks = BigDecimal.valueOf(PAYCHECKS_PER_YEAR);
        BigDecimal onePaycheck = total.divide(paychecks, 2, java.math.RoundingMode.HALF_UP);
        if (plan.hsaLimit == null) {
            return new HsaCheck(yours, employer, total, null, null, onePaycheck, true, null);
        }
        BigDecimal spare = plan.hsaLimit.subtract(total);
        BigDecimal employerWithExtra = employer.multiply(paychecks.add(BigDecimal.ONE)).divide(paychecks, 6, java.math.RoundingMode.HALF_UP);
        BigDecimal most = plan.hsaLimit.subtract(employerWithExtra)
                .divide(paychecks.add(BigDecimal.ONE), 2, java.math.RoundingMode.FLOOR).max(BigDecimal.ZERO);
        return new HsaCheck(yours, employer, total, plan.hsaLimit, spare, onePaycheck, spare.compareTo(onePaycheck) >= 0, most);
    }

    public void addPayItem(UUID planId, PaySection section, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        jdbc.update("""
                        INSERT INTO plan_pay_items (id, plan_id, section, name, sort)
                        VALUES (:id, :planId, :section, :name,
                                (SELECT coalesce(max(sort), 0) + 1 FROM plan_pay_items WHERE plan_id = :planId))
                        """,
                new MapSqlParameterSource()
                        .addValue("id", Db.id(UUID.randomUUID()))
                        .addValue("planId", Db.id(planId))
                        .addValue("section", section.dbValue)
                        .addValue("name", name.strip()));
    }

    public void removePayItem(UUID planId, UUID itemId) {
        jdbc.update("DELETE FROM plan_pay_items WHERE plan_id = :planId AND id = :id",
                new MapSqlParameterSource()
                        .addValue("planId", Db.id(planId))
                        .addValue("id", Db.id(itemId)));
    }

    // ---- Totals ------------------------------------------------------------------------------

    /**
     * A plan's numbers per year, worked out the way the planning sheet does (the page's JavaScript does the same
     * math live as you type):
     *
     *   taxable  = gross - before tax
     *   paycheck = taxable - taxes - after tax
     *   net      = paycheck - transfers - donations
     *   savings  = net - spending (monthly + yearly budgets)
     *   emergency fund = insurance + spending
     *   retirement     = every line marked retirement
     *
     * If the plan has no donation lines, its From-income budget lines count as donations.
     */
    public record Totals(BigDecimal gross, BigDecimal pretax, BigDecimal taxable, BigDecimal taxes, BigDecimal posttax,
                         BigDecimal paycheck, BigDecimal transfers, BigDecimal donations, BigDecimal net,
                         BigDecimal monthly, BigDecimal yearly, BigDecimal other, BigDecimal spending, BigDecimal savings,
                         BigDecimal insurance, BigDecimal emergencyFund, BigDecimal retirement) {

        public BigDecimal perMonth(BigDecimal perYear) {
            return perYear.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
        }

        /** Percent of gross salary, e.g. 8.0 for 8%. */
        public BigDecimal percentOfGross(BigDecimal perYear) {
            return gross.signum() == 0 ? BigDecimal.ZERO
                    : perYear.multiply(BigDecimal.valueOf(100)).divide(gross, 2, java.math.RoundingMode.HALF_UP);
        }
    }

    public Totals totals(Plan plan) {
        BigDecimal gross = plan.grossSalary;
        List<PlanPayItem> items = payItems(plan.id);

        Map<PaySection, BigDecimal> bySection = new EnumMap<>(PaySection.class);
        for (PaySection section : PaySection.values()) {
            bySection.put(section, BigDecimal.ZERO);
        }
        // Before-tax lines are always a percent of gross (taxable depends on them).
        for (PlanPayItem item : items) {
            if (item.section == PaySection.PRETAX) {
                bySection.merge(PaySection.PRETAX, item.amount(gross, gross), BigDecimal::add);
            }
        }
        BigDecimal taxable = gross.subtract(bySection.get(PaySection.PRETAX));
        BigDecimal retirement = BigDecimal.ZERO;
        for (PlanPayItem item : items) {
            BigDecimal amount = item.amount(gross, item.section == PaySection.PRETAX ? gross : taxable);
            if (item.section != PaySection.PRETAX) {
                bySection.merge(item.section, amount, BigDecimal::add);
            }
            if (item.retirement) {
                retirement = retirement.add(amount);
            }
        }

        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal yearly = BigDecimal.ZERO;
        BigDecimal incomeLines = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        for (PlanLine line : lines(plan.id)) {
            switch (line.group) {
                case MONTHLY -> monthly = monthly.add(line.amount);
                case YEARLY -> yearly = yearly.add(line.amount);
                case INCOME -> incomeLines = incomeLines.add(line.amount);
                case OTHER -> other = other.add(line.amount);
            }
        }

        boolean hasDonationLines = items.stream().anyMatch(item -> item.section == PaySection.DONATION);
        BigDecimal donations = hasDonationLines ? bySection.get(PaySection.DONATION) : incomeLines;
        BigDecimal paycheck = taxable.subtract(bySection.get(PaySection.TAX)).subtract(bySection.get(PaySection.POSTTAX));
        BigDecimal net = paycheck.subtract(bySection.get(PaySection.TRANSFER)).subtract(donations);
        BigDecimal spending = monthly.add(yearly);
        BigDecimal insurance = bySection.get(PaySection.INSURANCE);

        return new Totals(gross, bySection.get(PaySection.PRETAX), taxable, bySection.get(PaySection.TAX),
                bySection.get(PaySection.POSTTAX), paycheck, bySection.get(PaySection.TRANSFER), donations, net,
                monthly, yearly, other, spending, net.subtract(spending), insurance, insurance.add(spending), retirement);
    }

    /**
     * When a plan has donation lines, their total is what its From-income budget line (e.g. Donations) gets, so the
     * reports compare actual donations to the plan. Only done when there's exactly one such line.
     */
    public void syncDonations(UUID planId) {
        Plan plan = get(planId);
        List<PlanLine> incomeLines = lines(planId).stream().filter(line -> line.group == PlanGroup.INCOME).toList();
        boolean hasDonationLines = payItems(planId).stream().anyMatch(item -> item.section == PaySection.DONATION);
        if (hasDonationLines && incomeLines.size() == 1) {
            setAmounts(planId, Map.of(incomeLines.getFirst().categoryId, totals(plan).donations()));
        }
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
        plan.grossSalary = Tools.fromDatabaseInt(resultSet.getInt("gross_salary"));
        int hsaLimit = resultSet.getInt("hsa_limit");
        plan.hsaLimit = resultSet.wasNull() ? null : Tools.fromDatabaseInt(hsaLimit);
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
