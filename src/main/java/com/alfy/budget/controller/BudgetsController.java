package com.alfy.budget.controller;

import com.alfy.budget.model.*;
import com.alfy.budget.service.BudgetsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import com.alfy.budget.tools.Tools;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping(path = "/rest/budgets")
public class BudgetsController {

    public static final String NOT_CATEGORIZED = "Not Categorized";

    private final BudgetsService budgetsService;
    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;

    public BudgetsController(
            BudgetsService budgetsService,
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        this.budgetsService = budgetsService;
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public List<Budget> getBudgets() {
        return budgetsService.list();
    }

    @PostMapping(path = "/{id}/monthly")
    public void setMonthly(
            @PathVariable(name = "id") UUID id,
            @RequestBody(required = false) boolean monthly
    ) {
        budgetsService.setMonthly(id, monthly);
    }

    @PostMapping(path = "/{id}/amount")
    public void setAmount(
            @PathVariable(name = "id") UUID id,
            @RequestBody(required = false) BigDecimal amount
    ) {
        budgetsService.setAmount(id, amount);
    }

    @GetMapping(path = "/query-monthly")
    public List<BudgetInfo> getMonthlyBudgets(
            @RequestParam(name = "date", required = false) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());

        Map<UUID, BudgetInfo> budgetInfoById = new HashMap<>();
        Map<UUID, CategoryInfo> categoryInfoById = new HashMap<>();


        List<Budget> budgets = budgetsService.list();
        for (Budget budget : budgets) {
            if (budget.monthly) {
                BudgetInfo budgetInfo = new BudgetInfo();
                budgetInfo.budget = budget;
                budgetInfo.categories = new ArrayList<>();
                budgetInfoById.put(budget.id, budgetInfo);
            }
        }

        List<Category> categories = categoriesService.list();
        for (Category category : categories) {
            if (category.budget != null && category.budget.id != null) {
                BudgetInfo budgetInfo = budgetInfoById.get(category.budget.id);
                if (budgetInfo != null) {
                    CategoryInfo categoryInfo = new CategoryInfo();
                    categoryInfo.category = category;
                    categoryInfoById.put(category.id, categoryInfo);
                    budgetInfo.categories.add(categoryInfo);
                }
            }
        }

        List<Transaction> transactions = transactionsService.listByDate(start, end);
        for (Transaction transaction : transactions) {
            if (transaction.category != null && transaction.category.id != null) {
                CategoryInfo categoryInfo = categoryInfoById.get(transaction.category.id);
                if (categoryInfo != null) {
                    addTransactionToTotal(categoryInfo, transaction);

                    if (categoryInfo.transactions == null) {
                        categoryInfo.transactions = new ArrayList<>();
                    }
                    categoryInfo.transactions.add(transaction);
                }
            }
        }

        List<BudgetInfo> budgetInfoList = new ArrayList<>(budgetInfoById.values());
        budgetInfoList.sort(Comparator.comparing(budgetInfo -> budgetInfo.budget.name));

        for (BudgetInfo budgetInfo : budgetInfoList) {
            budgetInfo.total = BigDecimal.ZERO;
            for (CategoryInfo category : budgetInfo.categories) {
                budgetInfo.total = budgetInfo.total.add(category.total);
            }

            budgetInfo.percent = Tools.percentAsInt(budgetInfo.total, budgetInfo.budget.amount);
        }

        return budgetInfoList;
    }

    @GetMapping(path = "/query-yearly")
    public List<BudgetInfo> getYearlyBudgets(
            @RequestParam(name = "date", required = false) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate start = date.with(TemporalAdjusters.firstDayOfYear());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfYear());

        Map<UUID, BudgetInfo> budgetInfoById = new HashMap<>();
        Map<UUID, CategoryInfo> categoryInfoById = new HashMap<>();

        UUID noBudgetId = UUID.randomUUID();
        BudgetInfo noBudgetInfo = new BudgetInfo();
        noBudgetInfo.budget = new Budget();
        noBudgetInfo.budget.id = noBudgetId;
        noBudgetInfo.budget.name = "No Budget";
        noBudgetInfo.budget.amount = new BigDecimal(0);
        noBudgetInfo.categories = new ArrayList<>();

        List<Budget> budgets = budgetsService.list();
        for (Budget budget : budgets) {
            BudgetInfo budgetInfo = new BudgetInfo();
            budgetInfo.budget = budget;
            budgetInfo.categories = new ArrayList<>();
            budgetInfoById.put(budget.id, budgetInfo);
        }

        boolean foundBudget;
        List<Category> categories = categoriesService.list();
        for (Category category : categories) {
            foundBudget = false;

            if (category.budget != null && category.budget.id != null) {
                BudgetInfo budgetInfo = budgetInfoById.get(category.budget.id);
                if (budgetInfo != null) {
                    CategoryInfo categoryInfo = new CategoryInfo();
                    categoryInfo.category = category;
                    categoryInfoById.put(category.id, categoryInfo);
                    budgetInfo.categories.add(categoryInfo);
                    foundBudget = true;
                }
            }

            if (!foundBudget) {
                CategoryInfo categoryInfo = new CategoryInfo();
                categoryInfo.category = category;
                categoryInfoById.put(category.id, categoryInfo);
                noBudgetInfo.categories.add(categoryInfo);
            }
        }

        CategoryInfo noCategoryInfo = new CategoryInfo();
        noCategoryInfo.category = new Category();
        noCategoryInfo.category.id = UUID.randomUUID();
        noCategoryInfo.category.name = NOT_CATEGORIZED;
        noCategoryInfo.category.budget = noBudgetInfo.budget;
        noBudgetInfo.categories.add(noCategoryInfo);

        List<Transaction> transactions = transactionsService.listByDate(start, end);
        for (Transaction transaction : transactions) {
            if (transaction.category != null && transaction.category.id != null) {
                CategoryInfo categoryInfo = categoryInfoById.get(transaction.category.id);
                if (categoryInfo != null) {
                    addTransactionToTotal(categoryInfo, transaction);

                    if (categoryInfo.transactions == null) {
                        categoryInfo.transactions = new ArrayList<>();
                    }
                    categoryInfo.transactions.add(transaction);
                    continue;
                }
            }

            addTransactionToTotal(noCategoryInfo, transaction);
            if (noCategoryInfo.transactions == null) {
                noCategoryInfo.transactions = new ArrayList<>();
            }
            noCategoryInfo.transactions.add(transaction);
        }

        List<BudgetInfo> budgetInfoList = new ArrayList<>(budgetInfoById.values());
        budgetInfoList.sort(Comparator.comparing(budgetInfo -> budgetInfo.budget.name));
        budgetInfoList.add(noBudgetInfo);

        for (BudgetInfo budgetInfo : budgetInfoList) {
            budgetInfo.total = BigDecimal.ZERO;
            for (CategoryInfo category : budgetInfo.categories) {
                budgetInfo.total = budgetInfo.total.add(category.total);
            }

            budgetInfo.budget.amount = BigDecimal.valueOf(12).multiply(budgetInfo.budget.amount);

            BigDecimal dayOfYear = BigDecimal.valueOf(date.getDayOfYear());
            BigDecimal daysInYear = BigDecimal.valueOf(Year.of(date.getYear()).length());
            BigDecimal percentOfYear = dayOfYear.divide(daysInYear, 2, RoundingMode.HALF_UP);

            budgetInfo.expectedTotal = percentOfYear.multiply(budgetInfo.budget.amount);
            budgetInfo.expectedPercent = Tools.percentAsInt(budgetInfo.expectedTotal, budgetInfo.budget.amount);

            budgetInfo.percent = Tools.percentAsInt(budgetInfo.total, budgetInfo.budget.amount);
        }

        return budgetInfoList;
    }

    private static void addTransactionToTotal(CategoryInfo categoryInfo, Transaction transaction) {
        if (transaction.credit()) {
            categoryInfo.total = categoryInfo.total.subtract(transaction.amount);
        } else {
            categoryInfo.total = categoryInfo.total.add(transaction.amount);
        }
    }

}
