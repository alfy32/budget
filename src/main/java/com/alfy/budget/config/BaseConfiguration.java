package com.alfy.budget.config;

import com.alfy.budget.service.*;
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
        return new BudgetsService(namedParameterJdbcTemplate);
    }

    @Bean
    public CategoriesService categoriesService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new CategoriesService(namedParameterJdbcTemplate);
    }

    @Bean
    public TransactionsService transactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new TransactionsService(namedParameterJdbcTemplate);
    }

    @Bean
    public InitializerService initializerService(BudgetsService budgetsService, CategoriesService categoriesService) {
        InitializerService initializerService = new InitializerService(budgetsService, categoriesService);
        initializerService.addInitialBudgetsAndCategories();
        return initializerService;
    }

    @Bean
    public AutoCategorizeService autoCategorizeService(
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        return new AutoCategorizeService(categoriesService, transactionsService);
    }

}
