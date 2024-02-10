package com.alfy.budget;

import com.alfy.budget.service.TransactionsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class BaseConfiguration {

    @Bean
    public TransactionsService transactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new TransactionsService(namedParameterJdbcTemplate);
    }

}
