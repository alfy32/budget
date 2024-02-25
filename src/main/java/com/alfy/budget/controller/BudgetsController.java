package com.alfy.budget.controller;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping(path = "/rest/budgets")
public class BudgetsController {

    private final TransactionsService transactionsService;

    public BudgetsController(TransactionsService transactionsService) {
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

        List<Transaction> transactions = transactionsService.getTransactions(start, end);

        Map<String, Budget> totalsPerCategory = new HashMap<>();
        for (Transaction transaction : transactions) {
            String transactionCategory = transaction.category == null || transaction.category.isEmpty() ? "Not Categorized" : transaction.category;
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

}
