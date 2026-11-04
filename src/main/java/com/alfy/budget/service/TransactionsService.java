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
                    :description, :amount, 1
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

    public List<Transaction> listAll() {
        return jdbc.query("SELECT * FROM transactions" + ORDER, TransactionsService::map);
    }

    public int countNeedsCategorized() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM transactions WHERE category_id IS NULL", Map.of(), Integer.class);
        return count == null ? 0 : count;
    }

    public int countNeedsTransferred() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM transactions WHERE needs_transferred = 1", Map.of(), Integer.class);
        return count == null ? 0 : count;
    }

    public List<Transaction> listNeedsCategorized() {
        return jdbc.query("SELECT * FROM transactions WHERE category_id IS NULL" + ORDER, TransactionsService::map);
    }

    public List<Transaction> listNeedTransferred() {
        return jdbc.query("SELECT * FROM transactions WHERE needs_transferred = 1" + ORDER, TransactionsService::map);
    }

    public List<Transaction> listWithCategory(UUID categoryId, LocalDate startDate, LocalDate endDate) {
        String query = """
                SELECT * FROM transactions
                WHERE category_id = :categoryId
                  AND transaction_date >= :startDate
                  AND transaction_date <= :endDate
                """ + ORDER;

        return jdbc.query(query, new MapSqlParameterSource()
                .addValue("categoryId", Db.id(categoryId))
                .addValue("startDate", Db.date(startDate))
                .addValue("endDate", Db.date(endDate)), TransactionsService::map);
    }

    public List<Transaction> listWithCategoryNeedsTransferred(UUID categoryId) {
        String query = """
                SELECT * FROM transactions
                WHERE category_id = :categoryId
                  AND needs_transferred = 1
                """ + ORDER;

        return jdbc.query(query, new MapSqlParameterSource("categoryId", Db.id(categoryId)), TransactionsService::map);
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
        update(transactionId, "category_id", Db.id(categoryId));
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

    public void needsTransferred(UUID transactionId, boolean needsTransferred) {
        update(transactionId, "needs_transferred", needsTransferred ? 1 : 0);
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
                    split_index = :splitIndex
                WHERE id = :id
                """;

        jdbc.update(query, new MapSqlParameterSource()
                .addValue("id", Db.id(splitTransaction.id))
                .addValue("description", splitTransaction.description)
                .addValue("amount", Tools.toDatabaseInt(splitTransaction.amount))
                .addValue("splitIndex", splitTransaction.index)
                .addValue("categoryId", Db.id(splitTransaction.categoryId)));
    }

    public void addSplitTransaction(BankTransaction bankTransaction, SplitTransaction splitTransaction) {
        String query = """
                INSERT INTO transactions (
                    id, bank_transaction_id, split_index, account, transaction_type, transaction_date,
                    description, amount, category_id
                )
                VALUES (
                    :id, :bankTransactionId, :splitIndex, :account, :transactionType, :transactionDate,
                    :description, :amount, :categoryId
                )
                """;

        jdbc.update(query, new MapSqlParameterSource()
                .addValue("id", Db.id(UUID.randomUUID()))
                .addValue("bankTransactionId", Db.id(bankTransaction.id))
                .addValue("splitIndex", splitTransaction.index)
                .addValue("account", bankTransaction.account)
                .addValue("transactionType", bankTransaction.transactionType)
                .addValue("transactionDate", Db.date(bankTransaction.transactionDate))
                .addValue("description", splitTransaction.description)
                .addValue("amount", Tools.toDatabaseInt(splitTransaction.amount))
                .addValue("categoryId", Db.id(splitTransaction.categoryId)));
    }

    public int markTransactionsTransferredForAccount(String transferAccount) {
        if (transferAccount == null || transferAccount.isEmpty()) {
            return 0;
        }

        String query = """
                UPDATE transactions
                SET needs_transferred = 0
                WHERE category_id IN (
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
        return transaction;
    }
}
