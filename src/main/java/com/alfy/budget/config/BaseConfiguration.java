package com.alfy.budget.config;

import com.alfy.budget.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class BaseConfiguration {

    @Bean
    public BankTransactionsService bankTransactionsService(NamedParameterJdbcTemplate jdbc) {
        return new BankTransactionsService(jdbc);
    }

    @Bean
    public BudgetsService budgetsService(NamedParameterJdbcTemplate jdbc) {
        return new BudgetsService(jdbc);
    }

    @Bean
    public CategoriesService categoriesService(NamedParameterJdbcTemplate jdbc) {
        return new CategoriesService(jdbc);
    }

    @Bean
    public TransactionsService transactionsService(NamedParameterJdbcTemplate jdbc) {
        return new TransactionsService(jdbc);
    }

    @Bean
    public AutoCategorizeService autoCategorizeService(NamedParameterJdbcTemplate jdbc, TransactionsService transactionsService) {
        return new AutoCategorizeService(jdbc, transactionsService);
    }

    @Bean
    public AccountsService accountsService(NamedParameterJdbcTemplate jdbc, TransactionsService transactionsService) {
        return new AccountsService(jdbc, transactionsService);
    }

    @Bean
    public BudgetReportsService budgetReportsService(
            BudgetsService budgetsService,
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        return new BudgetReportsService(budgetsService, categoriesService, transactionsService);
    }

}
