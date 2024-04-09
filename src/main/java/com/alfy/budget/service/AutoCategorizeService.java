package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoCategorizeService {

    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;

    private Map<String, Category> categoriesByName;

    public AutoCategorizeService(
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
    }

    public void updateCachedData() {
        Map<String, Category> categoriesByName = new HashMap<>();
        for (Category category : categoriesService.list()) {
            categoriesByName.put(category.name, category);
        }

        this.categoriesByName = categoriesByName;
    }

    public boolean autoCategorize(
            BankTransaction bankTransaction,
            UUID transactionId
    ) {
        if (categoriesByName == null || categoriesByName.isEmpty()) {
            return false;
        }

        if ("VANGUARD BUY/INVESTMENT".equals(bankTransaction.description)) {
            Category category = categoriesByName.get("Investments");
            if (category != null) {
                transactionsService.updateCategory(transactionId, category.id);
                return true;
            }
        }

        if ("Check Payroll/PAY".equals(bankTransaction.description)) {
            Category category = categoriesByName.get("Paycheck");
            if (category != null) {
                transactionsService.updateCategory(transactionId, category.id);
                return true;
            }
        }

        return false;
    }

}
