package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Category;
import com.alfy.budget.model.SplitTransaction;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.tools.Tools;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransactionsService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TransactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public UUID addFrom(BankTransaction bankTransaction) {
        UUID id = UUID.randomUUID();

        String query = "INSERT INTO transactions (" +
                "  id," +
                "  bankTransactionId," +
                "  account," +
                "  transactionType," +
                "  transactionDate," +
                "  description," +
                "  amount" +
                ")" +
                "VALUES (" +
                "  :id," +
                "  :bankTransactionId," +
                "  :account," +
                "  :transactionType," +
                "  :transactionDate," +
                "  :description," +
                "  :amount" +
                ")";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("bankTransactionId", bankTransaction.id)
                .addValue("account", bankTransaction.account, Types.VARCHAR)
                .addValue("transactionType", bankTransaction.transactionType, Types.VARCHAR)
                .addValue("transactionDate", bankTransaction.transactionDate, Types.DATE)
                .addValue("description", bankTransaction.description, Types.VARCHAR)
                .addValue("amount", Tools.toDatabaseInt(bankTransaction.amount), Types.INTEGER);

        if (namedParameterJdbcTemplate.update(query, sqlParameterSource) == 1) {
            return id;
        } else {
            return null;
        }
    }

    public Transaction get(UUID id) {
        String query = "SELECT * FROM transactions WHERE id = :id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        return namedParameterJdbcTemplate.queryForObject(query, sqlParameterSource, TransactionsService::mapTransaction);
    }

    public void delete(UUID id) {
        String query = "DELETE FROM transactions WHERE id = :id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public Map<String, Object> getTransactionDescription(UUID id) {
        String query = "SELECT transactions.description, bank_transactions.description AS original_description "
                + " FROM transactions"
                + " INNER JOIN bank_transactions "
                + "    ON transactions.bankTransactionId = bank_transactions.id"
                + " WHERE transactions.id=:transactionId";
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("transactionId", id);
        Map<String, Object> stringObjectMap = namedParameterJdbcTemplate.queryForMap(query, paramMap);


        String originalDescription = (String) stringObjectMap.get("original_description");
        if (originalDescription == null) {
            originalDescription = "";
        }

        String decription = (String) stringObjectMap.get("description");
        if (decription == null) {
            decription = "";
        }

        HashMap<String, Object> json = new HashMap<>();
        json.put("description", decription);
        json.put("originalDescription", originalDescription);
        return json;
    }

    public List<Transaction> listOrderedByDate() {
        String query = "SELECT * FROM transactions" +
                " ORDER By transactionDate DESC, description";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource();
        return namedParameterJdbcTemplate.query(query, sqlParameterSource, TransactionsService::mapTransaction);
    }

    public List<Transaction> listByDate(LocalDate start, LocalDate end) {
        String query = "SELECT * FROM transactions" +
                " WHERE transactionDate >= :start" +
                "   AND transactionDate <= :end";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("start", Date.valueOf(start))
                .addValue("end", Date.valueOf(end));

        return namedParameterJdbcTemplate.query(query, sqlParameterSource, TransactionsService::mapTransaction);
    }

    public List<Transaction> listWithBankTransactionId(UUID bankTransactionId) {
        String query = "SELECT * FROM transactions" +
                " WHERE bankTransactionId = :bankTransactionId";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("bankTransactionId", bankTransactionId);

        return namedParameterJdbcTemplate.query(query, sqlParameterSource, TransactionsService::mapTransaction);
    }

    public void updateCategory(UUID transactionId, UUID categoryId) {
        String query = "UPDATE transactions"
                + " SET categoryId = :categoryId"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", transactionId)
                .addValue("categoryId", categoryId);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void updateDescription(UUID transactionId, String description) {
        String query = "UPDATE transactions"
                + " SET description = :description"
                + " WHERE id = :transactionId";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("transactionId", transactionId)
                .addValue("description", description, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void updateNotes(UUID transactionId, String notes) {
        String query = "UPDATE transactions"
                + " SET notes = :notes"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", transactionId)
                .addValue("notes", notes, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void updateTags(UUID transactionId, String tags) {
        String query = "UPDATE transactions"
                + " SET tags = :tags"
                + " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", transactionId)
                .addValue("tags", tags, Types.VARCHAR);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void updateTransaction(SplitTransaction splitTransaction) {
        String query = "UPDATE transactions" +
                " SET description = :description" +
                "   , categoryId = :categoryId" +
                "   , amount = :amount" +
                "   , splitIndex = :splitIndex" +
                " WHERE id = :id";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", splitTransaction.id)
                .addValue("description", splitTransaction.description, Types.VARCHAR)
                .addValue("amount", Tools.toDatabaseInt(splitTransaction.amount), Types.INTEGER)
                .addValue("splitIndex", splitTransaction.index, Types.INTEGER)
                .addValue("categoryId", splitTransaction.categoryId);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    public void addSplitTransaction(BankTransaction bankTransaction, SplitTransaction splitTransaction) {
        String query = "INSERT INTO transactions (" +
                "  id," +
                "  bankTransactionId," +
                "  splitIndex," +
                "  account," +
                "  transactionType," +
                "  transactionDate," +
                "  description," +
                "  amount," +
                "  categoryId" +
                ")" +
                "VALUES (" +
                "  :id," +
                "  :bankTransactionId," +
                "  :splitIndex," +
                "  :account," +
                "  :transactionType," +
                "  :transactionDate," +
                "  :description," +
                "  :amount," +
                "  :categoryId" +
                ")";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("bankTransactionId", bankTransaction.id)
                .addValue("splitIndex", splitTransaction.index)
                .addValue("account", bankTransaction.account, Types.VARCHAR)
                .addValue("transactionType", bankTransaction.transactionType, Types.VARCHAR)
                .addValue("transactionDate", bankTransaction.transactionDate, Types.DATE)
                .addValue("description", splitTransaction.description, Types.VARCHAR)
                .addValue("amount", Tools.toDatabaseInt(splitTransaction.amount), Types.INTEGER)
                .addValue("categoryId", splitTransaction.categoryId);

        namedParameterJdbcTemplate.update(query, sqlParameterSource);
    }

    private static Transaction mapTransaction(ResultSet resultSet, int rowNum) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.id = UUID.fromString(resultSet.getString("id"));
        transaction.bankTransaction = new BankTransaction();
        transaction.bankTransaction.id = UUID.fromString(resultSet.getString("bankTransactionId"));
        transaction.splitIndex = resultSet.getInt("splitIndex");
        transaction.account = resultSet.getString("account");
        transaction.transactionType = resultSet.getString("transactionType");
        transaction.transactionDate = resultSet.getDate("transactionDate").toLocalDate();
        transaction.description = resultSet.getString("description");
        transaction.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));

        String categoryId = resultSet.getString("categoryId");
        if (Strings.isNotBlank(categoryId)) {
            transaction.category = new Category();
            transaction.category.id = UUID.fromString(categoryId);
        }

        transaction.tags = resultSet.getString("tags");
        transaction.notes = resultSet.getString("notes");
        return transaction;
    }

}
