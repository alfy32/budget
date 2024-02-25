package com.alfy.budget.service;

import com.alfy.budget.model.Transaction;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionsService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TransactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Transaction getTransaction(int id) {
        String query = "SELECT * FROM transactions" +
                " WHERE id = :id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", id);

        return namedParameterJdbcTemplate.queryForObject(query, paramMap, TransactionsService::mapTransaction);
    }

    public Map<String, Object> getTransactionDescription(int id) {
        String query = "SELECT transactions.description, bank_transactions.description AS original_description "
                + " FROM transactions"
                + " INNER JOIN bank_transactions "
                + "    ON transactions.bank_transaction_id = bank_transactions.id"
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

    public List<Transaction> getTransactionsOrderedByDate() {
        String query = "SELECT *" +
                " FROM transactions" +
                " ORDER By transaction_date DESC";

        HashMap<String, Object> paramMap = new HashMap<>();
        return namedParameterJdbcTemplate.query(query, paramMap, TransactionsService::mapTransaction);
    }

    public List<Transaction> getTransactions(LocalDate start, LocalDate end) {
        String query = "SELECT * FROM transactions" +
                " WHERE transaction_date >= :start" +
                "   AND transaction_date <= :end";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("start", Date.valueOf(start));
        paramMap.put("end", Date.valueOf(end));

        return namedParameterJdbcTemplate.query(query, paramMap, TransactionsService::mapTransaction);
    }

    public void updateCategory(int transactionId, String category) {
        String s = "UPDATE transactions"
                + " SET category = :category"
                + " WHERE id = :id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", transactionId);
        paramMap.put("category", category);
        namedParameterJdbcTemplate.update(s, paramMap);
    }

    public void updateDescription(int transactionId, String description) {
        String s = "UPDATE transactions"
                + " SET description = :description"
                + " WHERE id = :transactionId";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("transactionId", transactionId);
        paramMap.put("description", description);
        namedParameterJdbcTemplate.update(s, paramMap);
    }

    public void updateNotes(int transactionId, String notes) {
        String s = "UPDATE transactions"
                + " SET notes = :notes"
                + " WHERE id = :id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", transactionId);
        paramMap.put("notes", notes);
        namedParameterJdbcTemplate.update(s, paramMap);
    }

    public void updateTags(int transactionId, String tags) {
        String s = "UPDATE transactions"
                + " SET tags = :tags"
                + " WHERE id = :id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", transactionId);
        paramMap.put("tags", tags);
        namedParameterJdbcTemplate.update(s, paramMap);
    }

    private static Transaction mapTransaction(ResultSet resultSet, int rowNum) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.id = resultSet.getInt("id");
        transaction.bankTransactionId = resultSet.getInt("bank_transaction_id");
        transaction.account = resultSet.getString("account");
        transaction.date = resultSet.getDate("transaction_date").toLocalDate();
        transaction.description = resultSet.getString("description");
        transaction.comments = resultSet.getString("comments");
        transaction.checkNumber = resultSet.getString("check_number");
        transaction.amount = resultSet.getInt("amount");
        transaction.category = resultSet.getString("category");
        transaction.tags = resultSet.getString("tags");
        transaction.notes = resultSet.getString("notes");
        return transaction;
    }

}
