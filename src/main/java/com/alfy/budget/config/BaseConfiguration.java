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
    public PlansService plansService(NamedParameterJdbcTemplate jdbc) {
        return new PlansService(jdbc);
    }

    /** A plan set up ahead of time becomes current when its month comes; catch up on start. */
    @Bean
    public org.springframework.boot.ApplicationRunner syncCurrentPlanOnStart(PlansService plansService) {
        return args -> plansService.syncCurrentPlan();
    }

    @Bean
    public BudgetReportsService budgetReportsService(
            BudgetsService budgetsService,
            CategoriesService categoriesService,
            TransactionsService transactionsService,
            PlansService plansService
    ) {
        return new BudgetReportsService(budgetsService, categoriesService, transactionsService, plansService);
    }

}
