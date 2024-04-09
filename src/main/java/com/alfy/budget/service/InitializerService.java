package com.alfy.budget.service;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;

import java.math.BigDecimal;
import java.util.List;

public class InitializerService {

    private final BudgetsService budgetsService;
    private final CategoriesService categoriesService;

    public InitializerService(BudgetsService budgetsService, CategoriesService categoriesService) {
        this.budgetsService = budgetsService;
        this.categoriesService = categoriesService;
    }

    public void addInitialBudgetsAndCategories() {
        List<Budget> budgets = budgetsService.list();
        List<Category> categories = categoriesService.list();

        if (budgets.isEmpty() && categories.isEmpty()) {
            Budget budget01 = budgetsService.add(Budget.create("Donations", new BigDecimal("1418.75")));
            Budget budget02 = budgetsService.add(Budget.create("Life Insurance", new BigDecimal("90")));
            Budget budget03 = budgetsService.add(Budget.create("Home & Auto Insurance", new BigDecimal("208.33")));
            Budget budget04 = budgetsService.add(Budget.create("Property Tax", new BigDecimal("183.33")));
            Budget budget05 = budgetsService.add(Budget.create("Car Registration", new BigDecimal("33.33")));
            Budget budget06 = budgetsService.add(Budget.create("Car Bills", new BigDecimal("33.33")));
            Budget budget07 = budgetsService.add(Budget.create("Gift", new BigDecimal("273.33")));
            Budget budget08 = budgetsService.add(Budget.create("Generous", new BigDecimal("125.00")));
            Budget budget09 = budgetsService.add(Budget.create("Bingo Chips", new BigDecimal("50.00")));
            Budget budget10 = budgetsService.add(Budget.create("Medical", new BigDecimal("416.67")));
            Budget budget11 = budgetsService.add(Budget.create("Bills & Utilities", new BigDecimal("700")));
            Budget budget12 = budgetsService.add(Budget.create("Groceries", new BigDecimal("700")));
            Budget budget13 = budgetsService.add(Budget.create("School Lunch", new BigDecimal("100")));
            Budget budget14 = budgetsService.add(Budget.create("Gas & Fuel", new BigDecimal("200")));
            Budget budget15 = budgetsService.add(Budget.create("Eating Out", new BigDecimal("100")));
            Budget budget16 = budgetsService.add(Budget.create("Food on Vacation", new BigDecimal("30")));
            Budget budget17 = budgetsService.add(Budget.create("Hotel", new BigDecimal("60")));
            Budget budget18 = budgetsService.add(Budget.create("Kid's Activities", new BigDecimal("250")));
            Budget budget19 = budgetsService.add(Budget.create("Clothing", new BigDecimal("137.83")));
            Budget budget20 = budgetsService.add(Budget.create("Pet Food", new BigDecimal("50")));
            Budget budget21 = budgetsService.add(Budget.create("Miscellaneous", new BigDecimal("500")));

            categoriesService.add(Category.create("Car Bills", budget06));
            categoriesService.add(Category.create("Cell Phone", budget11));
            categoriesService.add(Category.create("Credit Card Payment"));
            categoriesService.add(Category.create("Donations", budget01));
            categoriesService.add(Category.create("Eat Out", budget15));
            categoriesService.add(Category.create("Family Activities"));
            categoriesService.add(Category.create("Food on Vacation", budget16));
            categoriesService.add(Category.create("Gas & Fuel", budget14));
            categoriesService.add(Category.create("Generous", budget08));
            categoriesService.add(Category.create("Groceries", budget12));
            categoriesService.add(Category.create("Gunnison City", budget11));
            categoriesService.add(Category.create("Hotel", budget17));
            categoriesService.add(Category.create("Interest"));
            categoriesService.add(Category.create("Internet", budget11));
            categoriesService.add(Category.create("Investments"));
            categoriesService.add(Category.create("Kid Activity/Sport", budget18));
            categoriesService.add(Category.create("Medical", budget10));
            categoriesService.add(Category.create("Other Regular", budget21));
            categoriesService.add(Category.create("Paycheck"));
            categoriesService.add(Category.create("Pharmacy", budget10));
            categoriesService.add(Category.create("Registration"));
            categoriesService.add(Category.create("School Lunch", budget13));
            categoriesService.add(Category.create("Transfer"));
            categoriesService.add(Category.create("Utilities", budget11));
        }
    }

}
