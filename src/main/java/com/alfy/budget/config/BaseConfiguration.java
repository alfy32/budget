package com.alfy.budget.config;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.BudgetsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class BaseConfiguration {

    @Bean
    public BankTransactionsService bankTransactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new BankTransactionsService(namedParameterJdbcTemplate);
    }

    @Bean
    public BudgetsService budgetsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        BudgetsService budgetsService = new BudgetsService(namedParameterJdbcTemplate);
        if (budgetsService.list().isEmpty()) {
            budgetsService.add(Budget.create("Bills & Utilities", 700));
            budgetsService.add(Budget.create("Groceries", 700));
            budgetsService.add(Budget.create("School Lunch", 100));
            budgetsService.add(Budget.create("Gas", 200));
            budgetsService.add(Budget.create("Eating Out", 100));
            budgetsService.add(Budget.create("Food on Vacation", 30));
            budgetsService.add(Budget.create("Hotel", 60));
            budgetsService.add(Budget.create("Kid's Activities", 250));
            budgetsService.add(Budget.create("Clothing", 137.83));
            budgetsService.add(Budget.create("Pet Food", 50));
            budgetsService.add(Budget.create("Other Regular", 500));
        }
        return budgetsService;
    }

    @Bean
    public CategoriesService categoriesService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        CategoriesService categoriesService = new CategoriesService(namedParameterJdbcTemplate);
        if (categoriesService.list().isEmpty()) {
            categoriesService.add(Category.create("Car Bills"));
            categoriesService.add(Category.create("Credit Card Payment"));
            categoriesService.add(Category.create("Donations"));
            categoriesService.add(Category.create("Eat Out"));
            categoriesService.add(Category.create("Family Activities"));
            categoriesService.add(Category.create("Gas"));
            categoriesService.add(Category.create("Groceries"));
            categoriesService.add(Category.create("Interest"));
            categoriesService.add(Category.create("Internet"));
            categoriesService.add(Category.create("Investments"));
            categoriesService.add(Category.create("Kid Activity/Sport"));
            categoriesService.add(Category.create("Other Regular"));
            categoriesService.add(Category.create("Paycheck"));
            categoriesService.add(Category.create("Registration"));
            categoriesService.add(Category.create("School Lunch"));
            categoriesService.add(Category.create("Transfer"));
            categoriesService.add(Category.create("Utilities"));
        }
        return categoriesService;
    }

    @Bean
    public TransactionsService transactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new TransactionsService(namedParameterJdbcTemplate);
    }

}
