package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.CategorizeRule;
import com.alfy.budget.model.CategorizeRule.MatchType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Sets the category of transactions from the rules in the categorize_rules table.
 * A rule compares its text with the bank's description (is exactly / starts with / contains, ignoring case).
 * When several rules match, the most specific wins (see CategorizeRule.MOST_SPECIFIC_FIRST).
 * Rules run on new transactions when a bank CSV is uploaded, or on demand with applyToUncategorized.
 */
public class AutoCategorizeService {

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionsService transactionsService;

    private List<CategorizeRule> cachedRules = List.of();

    public AutoCategorizeService(NamedParameterJdbcTemplate jdbc, TransactionsService transactionsService) {
        this.jdbc = jdbc;
        this.transactionsService = transactionsService;
    }

    private static final String SELECT = """
            SELECT categorize_rules.*, categories.name AS category_name
            FROM categorize_rules
            JOIN categories ON categories.id = categorize_rules.category_id
            """;

    /** Sorted by text, for showing. */
    public List<CategorizeRule> list() {
        return jdbc.query(SELECT + " ORDER BY lower(categorize_rules.description)", AutoCategorizeService::map);
    }

    public CategorizeRule get(UUID id) {
        return jdbc.queryForObject(SELECT + " WHERE categorize_rules.id = :id",
                new MapSqlParameterSource("id", Db.id(id)), AutoCategorizeService::map);
    }

    public UUID add(String description, MatchType matchType, UUID categoryId) {
        if (description == null || description.isBlank() || categoryId == null) {
            return null;
        }

        UUID id = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO categorize_rules (id, description, match_type, category_id)
                        VALUES (:id, :description, :matchType, :categoryId)
                        """,
                params(id, description, matchType, categoryId));
        return id;
    }

    public void update(UUID id, String description, MatchType matchType, UUID categoryId) {
        if (description == null || description.isBlank() || categoryId == null) {
            return;
        }

        jdbc.update("""
                        UPDATE categorize_rules
                        SET description = :description, match_type = :matchType, category_id = :categoryId
                        WHERE id = :id
                        """,
                params(id, description, matchType, categoryId));
    }

    private static MapSqlParameterSource params(UUID id, String description, MatchType matchType, UUID categoryId) {
        return new MapSqlParameterSource()
                .addValue("id", Db.id(id))
                .addValue("description", description.strip())
                .addValue("matchType", (matchType == null ? MatchType.CONTAINS : matchType).dbValue)
                .addValue("categoryId", Db.id(categoryId));
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM categorize_rules WHERE id = :id", new MapSqlParameterSource("id", Db.id(id)));
    }

    // ---- Uploads -----------------------------------------------------------------------------

    /**
     * Call once before an upload so each transaction doesn't re-read the rules.
     */
    public void updateCachedData() {
        List<CategorizeRule> rules = new ArrayList<>(list());
        rules.sort(CategorizeRule.MOST_SPECIFIC_FIRST);
        this.cachedRules = rules;
    }

    public boolean autoCategorize(BankTransaction bankTransaction, UUID transactionId) {
        CategorizeRule rule = winner(cachedRules, bankTransaction.description);
        if (rule == null) {
            return false;
        }

        transactionsService.updateCategory(transactionId, rule.categoryId);
        return true;
    }

    /** The most specific of the rules (already sorted) that matches, or null. */
    private static CategorizeRule winner(List<CategorizeRule> sortedRules, String bankDescription) {
        for (CategorizeRule rule : sortedRules) {
            if (rule.matches(bankDescription)) {
                return rule;
            }
        }
        return null;
    }

    // ---- Preview and apply -------------------------------------------------------------------

    /**
     * What a rule would do to the transactions already in the database.
     *
     * @param matches         transactions this rule would categorize (where it's the most specific matching rule)
     * @param needsCategory   the ones among them that don't have a category yet
     * @param examples        the most common matching bank descriptions, with how many times each appears
     */
    public record Preview(int matches, int needsCategory, List<Example> examples) {
    }

    public record Example(String description, int count) {
    }

    private record Candidate(UUID transactionId, UUID categoryId, String bankDescription) {
    }

    /**
     * rule may be unsaved (id null) or an edited copy of a saved rule; the saved version of it is ignored.
     */
    public Preview preview(CategorizeRule rule) {
        List<Candidate> won = wonBy(rule);

        Map<String, Integer> counts = new HashMap<>();
        int needsCategory = 0;
        for (Candidate candidate : won) {
            counts.merge(candidate.bankDescription(), 1, Integer::sum);
            if (candidate.categoryId() == null) {
                needsCategory++;
            }
        }

        List<Example> examples = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> new Example(entry.getKey(), entry.getValue()))
                .toList();

        return new Preview(won.size(), needsCategory, examples);
    }

    /**
     * Gives the rule's category to the transactions it matches that don't have a category yet.
     * Transactions that already have a category are never changed.
     *
     * @return how many transactions were categorized
     */
    public int applyToUncategorized(UUID ruleId) {
        CategorizeRule rule = get(ruleId);
        int count = 0;
        for (Candidate candidate : wonBy(rule)) {
            if (candidate.categoryId() == null) {
                transactionsService.updateCategory(candidate.transactionId(), rule.categoryId);
                count++;
            }
        }
        return count;
    }

    /** Transactions where this rule is the most specific matching rule. */
    private List<Candidate> wonBy(CategorizeRule rule) {
        if (rule.description == null || rule.description.isBlank()) {
            return List.of();
        }

        List<CategorizeRule> rules = new ArrayList<>(list());
        rules.removeIf(other -> other.id != null && other.id.equals(rule.id));
        rules.add(rule);
        rules.sort(CategorizeRule.MOST_SPECIFIC_FIRST);

        List<Candidate> candidates = jdbc.query("""
                        SELECT transactions.id, transactions.category_id, bank_transactions.description
                        FROM transactions
                        JOIN bank_transactions ON bank_transactions.id = transactions.bank_transaction_id
                        """,
                (resultSet, rowNum) -> new Candidate(
                        Db.getId(resultSet, "id"),
                        Db.getId(resultSet, "category_id"),
                        resultSet.getString("description")));

        List<Candidate> won = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (winner(rules, candidate.bankDescription()) == rule) {
                won.add(candidate);
            }
        }
        return won;
    }

    private static CategorizeRule map(ResultSet resultSet, int rowNum) throws SQLException {
        CategorizeRule rule = new CategorizeRule();
        rule.id = Db.getId(resultSet, "id");
        rule.description = resultSet.getString("description");
        rule.matchType = MatchType.fromDb(resultSet.getString("match_type"));
        rule.categoryId = Db.getId(resultSet, "category_id");
        rule.categoryName = resultSet.getString("category_name");
        return rule;
    }

}
