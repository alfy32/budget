package com.alfy.budget.service;

import com.alfy.budget.model.*;
import com.alfy.budget.tools.Tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Totals transactions by category and budget for the monthly, yearly and needs-transferred pages.
 */
public class BudgetReportsService {

    public static final String NOT_CATEGORIZED = "Not Categorized";

    private final BudgetsService budgetsService;
    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;
    private final PlansService plansService;

    public BudgetReportsService(
            BudgetsService budgetsService,
            CategoriesService categoriesService,
            TransactionsService transactionsService,
            PlansService plansService
    ) {
        this.budgetsService = budgetsService;
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
        this.plansService = plansService;
    }

    /**
     * The watched budgets (budget.monthly) for the month containing date, measured against the plan in
     * effect that month.
     */
    public List<BudgetInfo> monthly(LocalDate date) {
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());

        Map<UUID, Budget> budgetsById = budgetsService.listBudgetsById();
        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        Plan plan = plansService.forMonth(start);
        if (plan == null) {
            return List.of();
        }

        Map<UUID, BudgetInfo> budgetInfos = new LinkedHashMap<>();
        Map<UUID, CategoryInfo> categoryInfos = new HashMap<>();
        for (PlanLine line : plansService.lines(plan.id)) {
            Budget budget = budgetsById.get(line.budgetId);
            if (budget == null || !budget.monthly) {
                continue;
            }
            BudgetInfo budgetInfo = budgetInfos.computeIfAbsent(budget.id, id -> newBudgetInfo(budget));
            CategoryInfo categoryInfo = newCategoryInfo(categoriesById.get(line.categoryId));
            categoryInfo.amount = perMonth(line.amount);
            budgetInfo.budget.amount = budgetInfo.budget.amount.add(categoryInfo.amount);
            budgetInfo.categories.add(categoryInfo);
            categoryInfos.put(line.categoryId, categoryInfo);
        }

        for (Transaction transaction : transactionsService.listByDate(start, end)) {
            if (transaction.category != null) {
                CategoryInfo categoryInfo = categoryInfos.get(transaction.category.id);
                if (categoryInfo != null) {
                    addTransactionToTotal(categoryInfo, transaction);
                }
            }
        }

        List<BudgetInfo> budgets = new ArrayList<>(budgetInfos.values());
        budgets.sort(Comparator.comparing(budgetInfo -> budgetInfo.budget.name.toLowerCase()));
        for (BudgetInfo budgetInfo : budgets) {
            finish(budgetInfo, null);
        }
        return budgets;
    }

    /**
     * Every budget for the year containing date. Each month counts against the plan in effect that month, so a
     * budget's amount for the year is the sum of its monthly amounts, and spending goes to whichever budget its
     * category was in that month. Categories in no budget that month show under "No Budget" (except ones that
     * belong to an account, which the Accounts page tracks).
     *
     * expectedTotal is what the plans add up to by today: whole months so far, plus the part of this month gone by.
     */
    public List<BudgetInfo> yearly(LocalDate date) {
        int year = date.getYear();
        LocalDate today = LocalDate.now();

        Map<UUID, Budget> budgetsById = budgetsService.listBudgetsById();
        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        Map<UUID, List<PlanLine>> linesByPlan = new HashMap<>();

        Map<UUID, BudgetInfo> budgetInfos = new HashMap<>();
        Map<String, CategoryInfo> categoryInfos = new HashMap<>();
        Map<UUID, BigDecimal> expectedByBudget = new HashMap<>();

        Budget noBudget = new Budget();
        noBudget.id = UUID.randomUUID();
        noBudget.name = "No Budget";
        BudgetInfo noBudgetInfo = newBudgetInfo(noBudget);
        noBudgetInfo.group = PlanGroup.OTHER;
        CategoryInfo notCategorized = newCategoryInfo(null);
        notCategorized.category = new Category();
        notCategorized.category.id = UUID.randomUUID();
        notCategorized.category.name = NOT_CATEGORIZED;

        Map<Integer, List<Transaction>> transactionsByMonth = new HashMap<>();
        for (Transaction transaction : transactionsService.listByDate(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))) {
            transactionsByMonth.computeIfAbsent(transaction.transactionDate.getMonthValue(), m -> new ArrayList<>()).add(transaction);
        }

        for (int month = 1; month <= 12; month++) {
            LocalDate first = LocalDate.of(year, month, 1);
            BigDecimal elapsed = elapsedPart(first, today);

            Plan plan = plansService.forMonth(first);
            List<PlanLine> lines = plan == null ? List.of() : linesByPlan.computeIfAbsent(plan.id, plansService::lines);
            Map<UUID, CategoryInfo> thisMonth = new HashMap<>();

            for (PlanLine line : lines) {
                Budget budget = budgetsById.get(line.budgetId);
                if (budget == null) {
                    continue;
                }
                BudgetInfo budgetInfo = budgetInfos.computeIfAbsent(budget.id, id -> newBudgetInfo(budget));
                CategoryInfo categoryInfo = categoryInfos.computeIfAbsent(budget.id + "/" + line.categoryId, key -> {
                    CategoryInfo info = newCategoryInfo(categoriesById.get(line.categoryId));
                    info.amount = BigDecimal.ZERO;
                    budgetInfo.categories.add(info);
                    return info;
                });

                BigDecimal perMonth = line.amount.divide(TWELVE, 6, RoundingMode.HALF_UP);
                categoryInfo.amount = categoryInfo.amount.add(perMonth);
                budgetInfo.budget.amount = budgetInfo.budget.amount.add(perMonth);
                expectedByBudget.merge(budget.id, perMonth.multiply(elapsed), BigDecimal::add);
                thisMonth.put(line.categoryId, categoryInfo);
            }

            for (Transaction transaction : transactionsByMonth.getOrDefault(month, List.of())) {
                Category category = transaction.category == null ? null : categoriesById.get(transaction.category.id);
                if (category == null) {
                    addTransactionToTotal(notCategorized, transaction);
                    continue;
                }

                CategoryInfo categoryInfo = thisMonth.get(category.id);
                if (categoryInfo == null) {
                    if (category.accountId != null) {
                        continue; // tracked on the Accounts page
                    }
                    categoryInfo = categoryInfos.computeIfAbsent("none/" + category.id, key -> {
                        CategoryInfo info = newCategoryInfo(category);
                        noBudgetInfo.categories.add(info);
                        return info;
                    });
                }
                addTransactionToTotal(categoryInfo, transaction);
            }
        }

        List<BudgetInfo> budgets = new ArrayList<>(budgetInfos.values());
        budgets.sort(Comparator
                .comparing((BudgetInfo budgetInfo) -> budgetInfo.group.ordinal())
                .thenComparing(budgetInfo -> budgetInfo.budget.name.toLowerCase()));
        noBudgetInfo.categories.sort(Comparator.comparing(info -> info.category.name.toLowerCase()));
        noBudgetInfo.categories.add(notCategorized);
        budgets.add(noBudgetInfo);

        for (BudgetInfo budgetInfo : budgets) {
            budgetInfo.categories.sort(Comparator.comparing(info -> info.category == notCategorized.category ? "\uffff" : info.category.name.toLowerCase()));
            finish(budgetInfo, expectedByBudget.getOrDefault(budgetInfo.budget.id, BigDecimal.ZERO));
        }
        return budgets;
    }

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private static BigDecimal perMonth(BigDecimal perYear) {
        return perYear.divide(TWELVE, 6, RoundingMode.HALF_UP);
    }

    /** How much of a month has gone by: 1 for past months, 0 for future ones, a fraction for this month. */
    private static BigDecimal elapsedPart(LocalDate firstOfMonth, LocalDate today) {
        LocalDate thisMonth = today.withDayOfMonth(1);
        if (firstOfMonth.isBefore(thisMonth)) {
            return BigDecimal.ONE;
        }
        if (firstOfMonth.isAfter(thisMonth)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(today.getDayOfMonth()).divide(BigDecimal.valueOf(today.lengthOfMonth()), 6, RoundingMode.HALF_UP);
    }

    private static BudgetInfo newBudgetInfo(Budget budget) {
        BudgetInfo budgetInfo = new BudgetInfo();
        // A copy, so the amount can be the plan's amount for this report.
        budgetInfo.budget = new Budget();
        budgetInfo.budget.id = budget.id;
        budgetInfo.budget.name = budget.name;
        budgetInfo.budget.monthly = budget.monthly;
        budgetInfo.budget.transferAccount = budget.transferAccount;
        budgetInfo.budget.group = budget.group;
        budgetInfo.budget.amount = BigDecimal.ZERO;
        budgetInfo.group = budget.group;
        budgetInfo.categories = new ArrayList<>();
        return budgetInfo;
    }

    private static CategoryInfo newCategoryInfo(Category category) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.category = category;
        return categoryInfo;
    }

    /** Rounds the amounts to cents and works out the totals and percents. */
    private static void finish(BudgetInfo budgetInfo, BigDecimal expected) {
        budgetInfo.total = BigDecimal.ZERO;
        for (CategoryInfo categoryInfo : budgetInfo.categories) {
            budgetInfo.total = budgetInfo.total.add(categoryInfo.total);
            if (categoryInfo.amount != null) {
                categoryInfo.amount = categoryInfo.amount.setScale(2, RoundingMode.HALF_UP);
            }
        }
        budgetInfo.budget.amount = budgetInfo.budget.amount.setScale(2, RoundingMode.HALF_UP);
        budgetInfo.percent = Tools.percentAsInt(budgetInfo.total, budgetInfo.budget.amount);
        if (expected != null) {
            budgetInfo.expectedTotal = expected.setScale(2, RoundingMode.HALF_UP);
            budgetInfo.expectedPercent = Tools.percentAsInt(budgetInfo.expectedTotal, budgetInfo.budget.amount);
        }
    }

    public List<TransferInfo> needsTransferred() {
        Map<String, TransferInfo> transferInfoByAccount = new HashMap<>();
        Map<UUID, BudgetInfo> budgetInfoById = new HashMap<>();
        Map<UUID, CategoryInfo> categoryInfoById = new HashMap<>();

        List<Budget> budgets = budgetsService.list();
        for (Budget budget : budgets) {
            BudgetInfo budgetInfo = new BudgetInfo();
            budgetInfo.budget = budget;
            budgetInfo.categories = new ArrayList<>();
            budgetInfoById.put(budget.id, budgetInfo);
            if (budget.transferAccount != null && !budget.transferAccount.isEmpty()) {
                TransferInfo transferInfo = transferInfoByAccount.computeIfAbsent(budget.transferAccount, (String account) -> {
                    TransferInfo newTransferInfo = new TransferInfo();
                    newTransferInfo.account = account;
                    newTransferInfo.amount = BigDecimal.ZERO;
                    newTransferInfo.budgets = new ArrayList<>();
                    return newTransferInfo;
                });
                transferInfo.budgets.add(budgetInfo);
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

        List<Transaction> transactions = transactionsService.listNeedTransferred();
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

        for (BudgetInfo budgetInfo : budgetInfoById.values()) {
            budgetInfo.total = BigDecimal.ZERO;
            for (CategoryInfo category : budgetInfo.categories) {
                budgetInfo.total = budgetInfo.total.add(category.total);
            }

            budgetInfo.budget.amount = BigDecimal.valueOf(12).multiply(budgetInfo.budget.amount);
            budgetInfo.percent = Tools.percentAsInt(budgetInfo.total, budgetInfo.budget.amount);
        }

        List<TransferInfo> transferInfoList = new ArrayList<>(transferInfoByAccount.values());
        transferInfoList.sort(Comparator.comparing(info -> info.account));

        for (TransferInfo transferInfo : transferInfoList) {
            transferInfo.amount = BigDecimal.ZERO;
            for (BudgetInfo budget : transferInfo.budgets) {
                transferInfo.amount = transferInfo.amount.add(budget.total);
            }
        }

        return transferInfoList;
    }

    private static void addTransactionToTotal(CategoryInfo categoryInfo, Transaction transaction) {
        if (transaction.credit()) {
            categoryInfo.total = categoryInfo.total.subtract(transaction.amount);
        } else {
            categoryInfo.total = categoryInfo.total.add(transaction.amount);
        }
    }

}
