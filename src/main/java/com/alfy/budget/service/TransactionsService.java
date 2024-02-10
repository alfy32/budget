package com.alfy.budget.service;

import com.alfy.budget.model.Transaction;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

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

    public List<Transaction> getTransactionsByDate() {
        String query = "SELECT *" +
                " FROM transactions" +
                " ORDER By transaction_date DESC";

        HashMap<String, Object> paramMap = new HashMap<>();
        return namedParameterJdbcTemplate.query(query, paramMap, TransactionsService::mapTransaction);
    }

    public List<Transaction> getTransactions(long start, long end) {
        String query = "SELECT * FROM transactions" +
                " WHERE date >= :start" +
                "   AND date <= :end";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("start", start);
        paramMap.put("end", end);

        return namedParameterJdbcTemplate.query(query, paramMap, TransactionsService::mapTransaction);
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
