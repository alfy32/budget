package com.alfy.budget.controller;

import com.alfy.budget.model.*;
import com.alfy.budget.service.BudgetsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.PlansService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Budget plans and the plan worksheet: every line with last plan's amount, what was actually spent in the
 * 12 months before the plan, and the new amount, with savings per month worked out live.
 */
@Controller
public class PlansController {

    private final PlansService plansService;
    private final BudgetsService budgetsService;
    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;

    public PlansController(
            PlansService plansService,
            BudgetsService budgetsService,
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        this.plansService = plansService;
        this.budgetsService = budgetsService;
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
    }

    @GetMapping("/plans")
    public String plans(Model model) {
        List<Plan> plans = new ArrayList<>(plansService.list());
        Collections.reverse(plans); // newest first
        Plan current = plansService.current();

        Map<UUID, PlansService.Totals> totals = new HashMap<>();
        for (Plan plan : plans) {
            totals.put(plan.id, plansService.totals(plan));
        }

        List<Plan> proposals = plansService.proposals();
        for (Plan plan : proposals) {
            totals.put(plan.id, plansService.totals(plan));
        }

        model.addAttribute("plans", plans);
        model.addAttribute("proposals", proposals);
        model.addAttribute("currentId", current == null ? null : current.id);
        model.addAttribute("totals", totals);
        model.addAttribute("nextMonth", YearMonth.now().plusMonths(1).toString());
        model.addAttribute("today", LocalDate.now());
        return "plans";
    }

    @GetMapping("/plans/current")
    public String current() {
        Plan current = plansService.current();
        return current == null ? "redirect:/plans" : "redirect:/plans/" + current.id;
    }

    /** Proposes a new plan: a copy of the current one with no start month, which nothing uses until it's committed. */
    @PostMapping("/plans")
    public String propose() {
        Plan current = plansService.current();
        if (current == null) {
            return "redirect:/plans";
        }
        UUID id = plansService.copy(current.id, null);
        return "redirect:/plans/" + id;
    }

    @GetMapping("/plans/{id}")
    public String plan(@PathVariable UUID id, @RequestParam(required = false) String error, Model model) {
        Plan plan = plansService.get(id);
        Plan previous = plansService.previous(plan);
        List<PlanLine> lines = plansService.lines(plan.id);

        Map<UUID, BigDecimal> lastAmounts = new HashMap<>();
        if (previous != null) {
            for (PlanLine line : plansService.lines(previous.id)) {
                lastAmounts.put(line.categoryId, line.amount);
            }
        }

        // What was actually spent in the 12 months before this plan started (for a proposal: the last 12 months).
        LocalDate before = plan.isProposed() ? LocalDate.now().withDayOfMonth(1) : plan.startMonth;
        LocalDate actualStart = before.minusYears(1);
        LocalDate actualEnd = before.minusDays(1);
        Map<UUID, BigDecimal> actual = transactionsService.spendingByCategory(actualStart, actualEnd);

        // Sheet order: from income, yearly, monthly, other.
        List<PlanGroup> groupOrder = List.of(PlanGroup.INCOME, PlanGroup.YEARLY, PlanGroup.MONTHLY, PlanGroup.OTHER);
        Map<PlanGroup, Map<UUID, List<PlanLine>>> grouped = new LinkedHashMap<>();
        for (PlanGroup group : groupOrder) {
            grouped.put(group, new LinkedHashMap<>());
        }
        for (PlanLine line : lines) {
            grouped.get(line.group).computeIfAbsent(line.budgetId, budgetId -> new ArrayList<>()).add(line);
        }

        // For the page's JavaScript: plain strings it can read directly.
        List<Map<String, String>> lineData = new ArrayList<>();
        for (PlanLine line : lines) {
            lineData.add(Map.of(
                    "id", line.categoryId.toString(),
                    "budget", line.budgetId.toString(),
                    "group", line.group.dbValue,
                    "amount", line.amount.toPlainString()));
        }
        List<PlanPayItem> payItems = plansService.payItems(plan.id);
        Map<PaySection, List<PlanPayItem>> payBySection = new LinkedHashMap<>();
        for (PaySection section : PaySection.values()) {
            payBySection.put(section, new ArrayList<>());
        }
        List<Map<String, String>> payData = new ArrayList<>();
        for (PlanPayItem item : payItems) {
            payBySection.get(item.section).add(item);
            payData.add(Map.of(
                    "id", item.id.toString(),
                    "section", item.section.dbValue,
                    "kind", item.percent ? (item.ofTaxable ? "taxable" : "gross") : "amount",
                    "value", item.value.toPlainString(),
                    "retirement", String.valueOf(item.retirement),
                    "hsa", String.valueOf(item.hsa)));
        }
        boolean hasDonationLines = !payBySection.get(PaySection.DONATION).isEmpty();

        Set<UUID> inPlan = new HashSet<>();
        lines.forEach(line -> inPlan.add(line.categoryId));
        List<Category> notInPlan = categoriesService.list().stream().filter(category -> !inPlan.contains(category.id)).toList();

        Plan current = plansService.current();
        model.addAttribute("title", plan.isProposed() ? "Proposed plan"
                : "Plan from " + plan.startMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)));
        model.addAttribute("plan", plan);
        model.addAttribute("previous", previous);
        model.addAttribute("isCurrent", current != null && current.id.equals(plan.id));
        model.addAttribute("startMonth", plan.isProposed() ? YearMonth.now().plusMonths(1).toString() : YearMonth.from(plan.startMonth).toString());
        model.addAttribute("grouped", grouped);
        model.addAttribute("budgetsById", budgetsService.listBudgetsById());
        model.addAttribute("budgets", budgetsService.list());
        model.addAttribute("groups", PlanGroup.values());
        model.addAttribute("lastAmounts", lastAmounts);
        model.addAttribute("actual", actual);
        model.addAttribute("actualLabel", YearMonth.from(actualStart) + " to " + YearMonth.from(actualEnd));
        model.addAttribute("payBySection", payBySection);
        model.addAttribute("hasDonationLines", hasDonationLines);
        model.addAttribute("payData", payData);
        model.addAttribute("notInPlan", notInPlan);
        model.addAttribute("lineData", lineData);
        model.addAttribute("error", error);
        return "plan";
    }

    /**
     * Saves the whole worksheet, then does the button's extra action (add/remove a line or income item).
     * Everything is saved first so nothing typed is lost.
     */
    @PostMapping("/plans/{id}")
    public String save(@PathVariable UUID id, @RequestParam Map<String, String> form) {
        Plan plan = plansService.get(id);

        plansService.update(plan.id, plan.startMonth,
                form.containsKey("grossSalary") ? money(form.get("grossSalary")) : plan.grossSalary, blankToNull(form.get("notes")));

        Map<UUID, BigDecimal> amounts = new HashMap<>();
        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (entry.getKey().startsWith("rename_")) {
                // Renames the category everywhere (it's the same category in every plan).
                UUID categoryId = UUID.fromString(entry.getKey().substring(7));
                String name = blankToNull(entry.getValue());
                Category category = categoriesById.get(categoryId);
                if (name != null && category != null && !name.equals(category.name)) {
                    categoriesService.setName(categoryId, name);
                }
                continue;
            }
            if (entry.getKey().startsWith("line_")) {
                amounts.put(UUID.fromString(entry.getKey().substring(5)), money(entry.getValue()));
            } else if (entry.getKey().startsWith("payValue_")) {
                UUID itemId = UUID.fromString(entry.getKey().substring("payValue_".length()));
                String kind = form.getOrDefault("payKind_" + itemId, "amount");
                plansService.updatePayItem(plan.id, itemId, form.get("payName_" + itemId), !kind.equals("amount"),
                        kind.equals("taxable"), money(entry.getValue()), form.containsKey("payRetirement_" + itemId),
                        form.containsKey("payHsa_" + itemId));
            } else if (entry.getKey().startsWith("budget_") && !entry.getValue().isBlank()) {
                plansService.putLine(plan.id, UUID.fromString(entry.getKey().substring(7)), UUID.fromString(entry.getValue()));
            }
        }
        plansService.setAmounts(plan.id, amounts);
        if (form.containsKey("hsaLimit")) {
            plansService.setHsaLimit(plan.id, moneyOrNull(form.get("hsaLimit")));
        }
        plansService.syncDonations(plan.id);

        String action = form.getOrDefault("action", "save");
        if (action.equals("addLine")) {
            UUID categoryId = uuidOrNull(form.get("addCategoryId"));
            UUID budgetId = uuidOrNull(form.get("addBudgetId"));
            String newBudget = blankToNull(form.get("newBudgetName"));
            if (newBudget != null) {
                Budget budget = budgetsService.add(Budget.create(newBudget, BigDecimal.ZERO));
                if (budget != null) {
                    budgetsService.setGroup(budget.id, PlanGroup.fromDb(form.getOrDefault("newBudgetGroup", "monthly")));
                    budgetId = budget.id;
                }
            }
            String newCategory = blankToNull(form.get("newCategoryName"));
            if (newCategory != null) {
                Category category = Category.create(newCategory);
                categoriesService.add(category);
                categoryId = category.id;
            }
            if (categoryId != null && budgetId != null) {
                plansService.putLine(plan.id, categoryId, budgetId);
            }
        } else if (action.startsWith("removeLine:")) {
            plansService.removeLine(plan.id, UUID.fromString(action.substring("removeLine:".length())));
        } else if (action.startsWith("split:")) {
            // Split off a new category: it joins this line's budget and takes that much of this line's amount.
            UUID categoryId = UUID.fromString(action.substring("split:".length()));
            String name = blankToNull(form.get("splitName_" + categoryId));
            BigDecimal amount = money(form.get("splitAmount_" + categoryId)).abs();
            PlanLine line = plansService.lines(plan.id).stream()
                    .filter(l -> l.categoryId.equals(categoryId)).findFirst().orElse(null);
            if (name == null || line == null) {
                return error("/plans/" + id, "Give the new category a name to split it off.");
            }
            Category category = Category.create(name);
            categoriesService.add(category);
            plansService.putLine(plan.id, category.id, line.budgetId);
            plansService.setAmounts(plan.id, Map.of(
                    category.id, amount.min(line.amount),
                    categoryId, line.amount.subtract(amount).max(BigDecimal.ZERO)));
        } else if (action.startsWith("addPay:")) {
            String section = action.substring("addPay:".length());
            plansService.addPayItem(plan.id, PaySection.fromDb(section), form.get("newPayName_" + section));
        } else if (action.startsWith("removePay:")) {
            plansService.removePayItem(plan.id, UUID.fromString(action.substring("removePay:".length())));
            if (form.containsKey("hsaLimit")) {
            plansService.setHsaLimit(plan.id, moneyOrNull(form.get("hsaLimit")));
        }
        plansService.syncDonations(plan.id);
        } else if (action.equals("start")) {
            // Commit a proposal to a start month (or move a plan's start month).
            LocalDate start = parseMonth(form.get("startMonth"));
            if (start == null) {
                return error("/plans/" + id, "Pick the month this plan starts.");
            }
            if (plansService.startMonthTaken(start, plan.id)) {
                return error("/plans/" + id, "There's already a plan starting " + YearMonth.from(start) + ". Delete or move that one first.");
            }
            Plan saved = plansService.get(plan.id);
            plansService.update(plan.id, start, saved.grossSalary, saved.notes);
            return "redirect:/plans/" + id + "?started=true";
        }

        return "redirect:/plans/" + id + (action.equals("save") ? "?saved=true" : "");
    }

    @PostMapping("/plans/{id}/delete")
    public String delete(@PathVariable UUID id) {
        if (!plansService.delete(id)) {
            return error("/plans/" + id, "This is the only plan, so it can't be deleted.");
        }
        return "redirect:/plans";
    }

    private static String error(String path, String message) {
        return "redirect:" + UriComponentsBuilder.fromPath(path).queryParam("error", message).encode().toUriString();
    }

    private static LocalDate parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value.strip()).atDay(1);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal money(String value) {
        BigDecimal parsed = moneyOrNull(value);
        return parsed == null ? BigDecimal.ZERO : parsed;
    }

    private static BigDecimal moneyOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replaceAll("[^0-9.-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static UUID uuidOrNull(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

}
