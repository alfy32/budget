package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

public class BankTransactionsService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public BankTransactionsService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public boolean exists(String account, String csv) {
        if (account == null || account.isEmpty()) {
            return false;
        }

        if (csv == null || csv.isEmpty()) {
            return false;
        }

        String query = "SELECT id FROM bank_transactions"
                + " WHERE account=:account"
                + "   AND csv=:csv";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("account", account, Types.VARCHAR)
                .addValue("csv", csv, Types.VARCHAR);

        SqlRowSet sqlRowSet = namedParameterJdbcTemplate.queryForRowSet(query, sqlParameterSource);
        return sqlRowSet.next();
    }

    public BankTransaction get(UUID id) {
        String query = "SELECT * FROM bank_transactions WHERE id = :id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        return namedParameterJdbcTemplate.queryForObject(query, sqlParameterSource, BankTransactionsService::map);

    }

    public boolean add(BankTransaction bankTransaction) {
        if (bankTransaction.id == null) {
            bankTransaction.id = UUID.randomUUID();
        }

        String sql = "INSERT INTO bank_transactions (" +
                "  id," +
                "  csv," +
                "  account," +
                "  transactionDate," +
                "  postDate," +
                "  description," +
                "  comments," +
                "  checkNumber," +
                "  amount" +
                ")" +
                "VALUES (" +
                "  :id," +
                "  :csv," +
                "  :account," +
                "  :transactionDate," +
                "  :postDate," +
                "  :description," +
                "  :comments," +
                "  :checkNumber," +
                "  :amount" +
                ")";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", bankTransaction.id)
                .addValue("csv", bankTransaction.csv, Types.VARCHAR)
                .addValue("account", bankTransaction.account, Types.VARCHAR)
                .addValue("transactionDate", bankTransaction.transactionDate, Types.DATE)
                .addValue("postDate", bankTransaction.postDate, Types.DATE)
                .addValue("description", bankTransaction.description, Types.VARCHAR)
                .addValue("comments", bankTransaction.comments, Types.VARCHAR)
                .addValue("checkNumber", bankTransaction.checkNumber, Types.VARCHAR)
                .addValue("amount", bankTransaction.amount, Types.INTEGER);

        return namedParameterJdbcTemplate.update(sql, sqlParameterSource) == 1;
    }

    private static BankTransaction map(ResultSet resultSet, int i) throws SQLException {
        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.id = UUID.fromString(resultSet.getString("id"));
        bankTransaction.csv = resultSet.getString("csv");
        bankTransaction.account = resultSet.getString("account");
        bankTransaction.transactionDate = resultSet.getDate("transactionDate").toLocalDate();
        Date postDate = resultSet.getDate("postDate");
        if (postDate != null) {
            bankTransaction.postDate = postDate.toLocalDate();
        }
        bankTransaction.description = resultSet.getString("description");
        bankTransaction.comments = resultSet.getString("comments");
        bankTransaction.checkNumber = resultSet.getString("checkNumber");
        bankTransaction.amount = resultSet.getInt("amount");
        return bankTransaction;
    }

}
