package com.alfy.budget.service;

import com.alfy.budget.model.*;
import com.alfy.budget.tools.Tools;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class TransactionsService {

    private static final String ORDER = " ORDER BY transaction_date DESC, description";

    /*
     * Transfers: spending comes out of checking. When a transaction's budget has a transfer account
     * (e.g. "Yearly"), checking gets paid back from that account. needs_transferred = 1 means it hasn't
     * been yet; transferred_from records the account that did pay it back. Choosing a category sets
     * needs_transferred from the category's budget, unless the transaction was already paid back.
     */

    /** SQL for the transfer account of a category (null if its budget has none). %s is the category id. */
    private static final String TRANSFER_ACCOUNT_OF = """
            (SELECT nullif(budgets.transfer_account, '')
             FROM categories JOIN budgets ON budgets.id = categories.budget_id
             WHERE categories.id = %s)""";

    /** needs_transferred after choosing category :categoryId for an existing transaction. */
    private static final String NEEDS_TRANSFERRED_FOR_NEW_CATEGORY =
            "CASE WHEN transferred_from IS NOT NULL THEN 0 WHEN " + TRANSFER_ACCOUNT_OF.formatted(":categoryId") + " IS NOT NULL THEN 1 ELSE 0 END";

    /** Transactions waiting for a transfer, in a budget that has a transfer account. */
    private static final String WAITING_FOR_TRANSFER = "needs_transferred = 1 AND " + TRANSFER_ACCOUNT_OF.formatted("transactions.category_id") + " IS NOT NULL";

    private final NamedParameterJdbcTemplate jdbc;

    public TransactionsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID addFrom(BankTransaction bankTransaction) {
        UUID id = UUID.randomUUID();

        String query = """
                INSERT INTO transactions (
                    id, bank_transaction_id, account, transaction_type, transaction_date,
                    description, amount, needs_transferred
                )
                VALUES (
                    :id, :bankTransactionId, :account, :transactionType, :transactionDate,
                    :description, :amount, 0
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", Db.id(id))
                .addValue("bankTransactionId", Db.id(bankTransaction.id))
                .addValue("account", bankTransaction.account)
                .addValue("transactionType", bankTransaction.transactionType)
                .addValue("transactionDate", Db.date(bankTransaction.transactionDate))
                .addValue("description", bankTransaction.description)
                .addValue("amount", Tools.toDatabaseInt(bankTransaction.amount));

        return jdbc.update(query, params) == 1 ? id : null;
    }

    public Transaction get(UUID id) {
        return jdbc.queryForObject("SELECT * FROM transactions WHERE id = :id",
                new MapSqlParameterSource("id", Db.id(id)), TransactionsService::map);
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM transactions WHERE id = :id", new MapSqlParameterSource("id", Db.id(id)));
    }

    public List<Transaction> listPossibleDuplicates(List<PossibleDuplicateTransactions> possibleDuplicateTransactions) {
        List<Transaction> possibleDuplicates = new ArrayList<>();

        for (PossibleDuplicateTransactions duplicateTransactions : possibleDuplicateTransactions) {
            List<Transaction> transactions1 = listWithBankTransactionId(duplicateTransactions.transaction1.id);
            List<Transaction> transactions2 = listWithBankTransactionId(duplicateTransactions.transaction2.id);
            if (!transactions1.isEmpty() && !transactions2.isEmpty()) {
                for (Transaction transaction : transactions1) {
                    transaction.bankTransaction = duplicateTransactions.transaction1;
                    possibleDuplicates.add(transaction);
                }

                for (Transaction transaction : transactions2) {
                    transaction.bankTransaction = duplicateTransactions.transaction2;
                    possibleDuplicates.add(transaction);
                }
            }
        }

        return possibleDuplicates;
    }

    /**
     * One page of a transaction list, newest first. search matches description or notes, ignoring case.
     */
    public List<Transaction> page(TransactionFilter filter, String search, int limit, int offset) {
        StringBuilder query = new StringBuilder("SELECT * FROM transactions WHERE 1 = 1");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        if (filter.needsCategorized()) {
            query.append(" AND category_id IS NULL");
        }
        if (filter.needsTransferred()) {
            query.append(" AND ").append(WAITING_FOR_TRANSFER);
        }
        if (filter.categoryId() != null) {
            query.append(" AND category_id = :categoryId");
            params.addValue("categoryId", Db.id(filter.categoryId()));
        }
        if (filter.start() != null) {
            query.append(" AND transaction_date >= :start");
            params.addValue("start", Db.date(filter.start()));
        }
        if (filter.end() != null) {
            query.append(" AND transaction_date <= :end");
            params.addValue("end", Db.date(filter.end()));
        }
        if (search != null && !search.isBlank()) {
            // SQLite's LIKE ignores case. Escape % and _ so they're searched for literally.
            String pattern = "%" + search.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            query.append(" AND (description LIKE :search ESCAPE '\\' OR notes LIKE :search ESCAPE '\\')");
            params.addValue("search", pattern);
        }

        // id last so the order never changes between pages
        query.append(" ORDER BY transaction_date DESC, description, id LIMIT :limit OFFSET :offset");

        return jdbc.query(query.toString(), params, TransactionsService::map);
    }

    public int countNeedsCategorized() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM transactions WHERE category_id IS NULL", Map.of(), Integer.class);
        return count == null ? 0 : count;
    }

    /** How many transactions wait for a transfer, and the total to move (debits add, credits subtract). */
    public record TransferSummary(int count, BigDecimal amount) {
    }

    public TransferSummary transferSummary() {
        return jdbc.queryForObject("""
                        SELECT count(*) AS count,
                               coalesce(sum(CASE WHEN transaction_type = 'credit' THEN -amount ELSE amount END), 0) AS cents
                        FROM transactions
                        WHERE %s
                        """.formatted(WAITING_FOR_TRANSFER), Map.of(),
                (resultSet, rowNum) -> new TransferSummary(resultSet.getInt("count"), Tools.fromDatabaseInt(resultSet.getInt("cents"))));
    }

    public List<Transaction> listNeedTransferred() {
        return jdbc.query("SELECT * FROM transactions WHERE needs_transferred = 1" + ORDER, TransactionsService::map);
    }

    /** Oldest first, so a running balance can be added up in order. */
    public List<Transaction> listForAccount(UUID accountId) {
        String query = """
                SELECT transactions.* FROM transactions
                JOIN categories ON categories.id = transactions.category_id
                WHERE categories.account_id = :accountId
                ORDER BY transactions.transaction_date, transactions.description, transactions.id
                """;

        return jdbc.query(query, new MapSqlParameterSource("accountId", Db.id(accountId)), TransactionsService::map);
    }

    public List<Transaction> listByDate(LocalDate start, LocalDate end) {
        String query = """
                SELECT * FROM transactions
                WHERE transaction_date >= :start
                  AND transaction_date <= :end
                """;

        return jdbc.query(query, new MapSqlParameterSource()
                .addValue("start", Db.date(start))
                .addValue("end", Db.date(end)), TransactionsService::map);
    }

    public List<Transaction> listWithBankTransactionId(UUID bankTransactionId) {
        return jdbc.query("SELECT * FROM transactions WHERE bank_transaction_id = :id ORDER BY split_index",
                new MapSqlParameterSource("id", Db.id(bankTransactionId)), TransactionsService::map);
    }

    public void updateCategory(UUID transactionId, UUID categoryId) {
        jdbc.update("UPDATE transactions SET category_id = :categoryId, needs_transferred = " + NEEDS_TRANSFERRED_FOR_NEW_CATEGORY + " WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(transactionId))
                        .addValue("categoryId", Db.id(categoryId)));
    }

    /** The account this transaction's budget transfers from, or null. */
    public String transferAccountFor(UUID transactionId) {
        List<String> accounts = jdbc.queryForList(
                "SELECT " + TRANSFER_ACCOUNT_OF.formatted("transactions.category_id") + " FROM transactions WHERE id = :id",
                new MapSqlParameterSource("id", Db.id(transactionId)), String.class);
        return accounts.isEmpty() ? null : accounts.getFirst();
    }

    public void updateAmount(UUID transactionId, BigDecimal amount) {
        update(transactionId, "amount", Tools.toDatabaseInt(amount));
    }

    public void updateDescription(UUID transactionId, String description) {
        update(transactionId, "description", description);
    }

    public void updateNotes(UUID transactionId, String notes) {
        update(transactionId, "notes", notes == null || notes.isBlank() ? null : notes);
    }

    public void updateDate(UUID transactionId, LocalDate transactionDate) {
        update(transactionId, "transaction_date", Db.date(transactionDate));
    }

    public void updateType(UUID transactionId, String transactionType) {
        update(transactionId, "transaction_type", transactionType);
    }

    public void updateTags(UUID transactionId, String tags) {
        update(transactionId, "tags", tags == null || tags.isBlank() ? null : tags);
    }

    /** Switched on or off by hand on a transaction's page. Off counts as paid back from its budget's account. */
    public void needsTransferred(UUID transactionId, boolean needsTransferred) {
        String set = needsTransferred
                ? "needs_transferred = 1, transferred_from = NULL"
                : "needs_transferred = 0, transferred_from = " + TRANSFER_ACCOUNT_OF.formatted("transactions.category_id");
        jdbc.update("UPDATE transactions SET " + set + " WHERE id = :id", new MapSqlParameterSource("id", Db.id(transactionId)));
    }

    private void update(UUID transactionId, String column, Object value) {
        jdbc.update("UPDATE transactions SET " + column + " = :value WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(transactionId))
                        .addValue("value", value));
    }

    public void updateTransaction(SplitTransaction splitTransaction) {
        String query = """
                UPDATE transactions
                SET description = :description,
                    category_id = :categoryId,
                    amount = :amount,
                    split_index = :splitIndex,
                    needs_transferred = %s
                WHERE id = :id
                """.formatted(NEEDS_TRANSFERRED_FOR_NEW_CATEGORY);

        jdbc.update(query, new MapSqlParameterSource()
                .addValue("id", Db.id(splitTransaction.id))
                .addValue("description", splitTransaction.description)
                .addValue("amount", Tools.toDatabaseInt(splitTransaction.amount))
                .addValue("splitIndex", splitTransaction.index)
                .addValue("categoryId", Db.id(splitTransaction.categoryId)));
    }

    /**
     * transferredFrom: the account the original transaction was already paid back from, or null. A new split
     * row of an already paid-back transaction counts as paid back too.
     */
    public void addSplitTransaction(BankTransaction bankTransaction, SplitTransaction splitTransaction, String transferredFrom) {
        String query = """
                INSERT INTO transactions (
                    id, bank_transaction_id, split_index, account, transaction_type, transaction_date,
                    description, amount, category_id, transferred_from, needs_transferred
                )
                VALUES (
                    :id, :bankTransactionId, :splitIndex, :account, :transactionType, :transactionDate,
                    :description, :amount, :categoryId, :transferredFrom,
                    CASE WHEN :transferredFrom IS NOT NULL THEN 0 WHEN %s IS NOT NULL THEN 1 ELSE 0 END
                )
                """.formatted(TRANSFER_ACCOUNT_OF.formatted(":categoryId"));

        jdbc.update(query, new MapSqlParameterSource()
                .addValue("id", Db.id(UUID.randomUUID()))
                .addValue("bankTransactionId", Db.id(bankTransaction.id))
                .addValue("splitIndex", splitTransaction.index)
                .addValue("account", bankTransaction.account)
                .addValue("transactionType", bankTransaction.transactionType)
                .addValue("transactionDate", Db.date(bankTransaction.transactionDate))
                .addValue("description", splitTransaction.description)
                .addValue("amount", Tools.toDatabaseInt(splitTransaction.amount))
                .addValue("categoryId", Db.id(splitTransaction.categoryId))
                .addValue("transferredFrom", transferredFrom));
    }

    public int markTransactionsTransferredForAccount(String transferAccount) {
        if (transferAccount == null || transferAccount.isEmpty()) {
            return 0;
        }

        String query = """
                UPDATE transactions
                SET needs_transferred = 0, transferred_from = :transferAccount
                WHERE needs_transferred = 1
                  AND category_id IN (
                    SELECT categories.id FROM categories
                    JOIN budgets ON budgets.id = categories.budget_id
                    WHERE budgets.transfer_account = :transferAccount
                )
                """;

        return jdbc.update(query, new MapSqlParameterSource("transferAccount", transferAccount));
    }

    private static Transaction map(ResultSet resultSet, int rowNum) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.id = Db.getId(resultSet, "id");
        transaction.bankTransaction = new BankTransaction();
        transaction.bankTransaction.id = Db.getId(resultSet, "bank_transaction_id");
        transaction.splitIndex = resultSet.getInt("split_index");
        transaction.account = resultSet.getString("account");
        transaction.transactionType = resultSet.getString("transaction_type");
        transaction.transactionDate = Db.getDate(resultSet, "transaction_date");
        transaction.description = resultSet.getString("description");
        transaction.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));

        UUID categoryId = Db.getId(resultSet, "category_id");
        if (categoryId != null) {
            transaction.category = new Category();
            transaction.category.id = categoryId;
        }

        transaction.tags = resultSet.getString("tags");
        transaction.notes = resultSet.getString("notes");
        transaction.needsTransferred = resultSet.getBoolean("needs_transferred");
        transaction.transferredFrom = resultSet.getString("transferred_from");
        return transaction;
    }
}
