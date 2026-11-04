package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.CategorizeRule;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sets the category of newly uploaded transactions using the rules in the categorize_rules table.
 * A rule matches when the bank's description is exactly equal to the rule's description.
 */
public class AutoCategorizeService {

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionsService transactionsService;

    private Map<String, UUID> categoryIdByDescription = Map.of();

    public AutoCategorizeService(NamedParameterJdbcTemplate jdbc, TransactionsService transactionsService) {
        this.jdbc = jdbc;
        this.transactionsService = transactionsService;
    }

    public List<CategorizeRule> list() {
        String query = """
                SELECT categorize_rules.*, categories.name AS category_name
                FROM categorize_rules
                JOIN categories ON categories.id = categorize_rules.category_id
                ORDER BY categorize_rules.description
                """;

        return jdbc.query(query, (resultSet, rowNum) -> {
            CategorizeRule rule = new CategorizeRule();
            rule.id = Db.getId(resultSet, "id");
            rule.description = resultSet.getString("description");
            rule.categoryId = Db.getId(resultSet, "category_id");
            rule.categoryName = resultSet.getString("category_name");
            return rule;
        });
    }

    public void add(String description, UUID categoryId) {
        if (description == null || description.isBlank() || categoryId == null) {
            return;
        }

        jdbc.update("INSERT INTO categorize_rules (id, description, category_id) VALUES (:id, :description, :categoryId)",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(UUID.randomUUID()))
                        .addValue("description", description.strip())
                        .addValue("categoryId", Db.id(categoryId)));
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM categorize_rules WHERE id = :id", new MapSqlParameterSource("id", Db.id(id)));
    }

    /**
     * Call once before an upload so each transaction doesn't re-read the rules.
     */
    public void updateCachedData() {
        Map<String, UUID> rules = new HashMap<>();
        for (CategorizeRule rule : list()) {
            rules.put(rule.description, rule.categoryId);
        }
        this.categoryIdByDescription = rules;
    }

    public boolean autoCategorize(BankTransaction bankTransaction, UUID transactionId) {
        if (bankTransaction.description == null) {
            return false;
        }

        UUID categoryId = categoryIdByDescription.get(bankTransaction.description);
        if (categoryId == null) {
            return false;
        }

        transactionsService.updateCategory(transactionId, categoryId);
        return true;
    }

}
