package com.alfy.budget.controller;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping(path = "/rest/budgets")
public class BudgetsController {

    public static final String NOT_CATEGORIZED = "Not Categorized";

    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;

    public BudgetsController(
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public List<Budget> getBudgets(
            @RequestParam(name = "date", required = false) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.withDayOfMonth(date.getMonth().length(date.isLeapYear()));

        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        List<Transaction> transactions = transactionsService.getTransactions(start, end);

        Map<String, Budget> totalsPerCategory = new HashMap<>();
        Budget notCategorized = new Budget();
        notCategorized.name = NOT_CATEGORIZED;
        totalsPerCategory.put(notCategorized.name, notCategorized);
        for (Transaction transaction : transactions) {
            String transactionCategory = getCategory(transaction, categoriesById);
            Budget budget = totalsPerCategory.computeIfAbsent(transactionCategory, key -> {
                Budget newBudget = new Budget();
                newBudget.name = key;
                return newBudget;
            });
            budget.total += transaction.amount;
        }

        List<Budget> budgets = new ArrayList<>(totalsPerCategory.values());
        budgets.sort(Comparator.comparing(budget -> budget.name));
        return budgets;
    }

    private static String getCategory(Transaction transaction, Map<UUID, Category> categoriesById) {
        if (transaction.category != null && transaction.category.id != null) {
            Category category = categoriesById.get(transaction.category.id);
            if (category != null && Strings.isNotBlank(category.name)) {
                return category.name;
            }
        }

        return NOT_CATEGORIZED;
    }

}
