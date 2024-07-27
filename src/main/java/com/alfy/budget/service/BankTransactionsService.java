package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.tools.Tools;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public boolean exists(String account, BankTransaction bankTransaction) {
        if (account == null || account.isEmpty()) {
            return false;
        }

        if (bankTransaction == null) {
            return false;
        }

        String query = """
                SELECT id FROM bank_transactions
                WHERE account=:account
                  AND transactionType=:transactionType
                  AND description=:description
                  AND amount=:amount
                  AND balance=:balance
                """;

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("account", bankTransaction.account, Types.VARCHAR)
                .addValue("transactionType", bankTransaction.transactionType, Types.VARCHAR)
                .addValue("description", bankTransaction.description, Types.VARCHAR)
                .addValue("amount", Tools.toDatabaseInt(bankTransaction.amount), Types.INTEGER)
                .addValue("balance", Tools.toDatabaseInt(bankTransaction.balance), Types.INTEGER);

        SqlRowSet sqlRowSet = namedParameterJdbcTemplate.queryForRowSet(query, sqlParameterSource);
        return sqlRowSet.next();
    }

    public BankTransaction get(UUID id) {
        String query = "SELECT * FROM bank_transactions WHERE id = :id";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        return namedParameterJdbcTemplate.queryForObject(query, sqlParameterSource, BankTransactionsService::map);
    }

    public List<BankTransaction> listPossibleDuplicates() {
        List<BankTransaction> duplicates = new ArrayList<>();

        String query = "SELECT * FROM bank_transactions WHERE transactionDate > :transactionDate";

        LocalDate startDate = LocalDate.now().minusMonths(12);

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("transactionDate", startDate, Types.DATE);

        List<BankTransaction> transactions = namedParameterJdbcTemplate.query(query, sqlParameterSource, BankTransactionsService::map);

        for (int i = 0; i < transactions.size() - 1; i++) {
            BankTransaction transaction1 = transactions.get(i);
            for (int j = i + 1; j < transactions.size(); j++) {
                BankTransaction transaction2 = transactions.get(j);
                if (isDuplicate(transaction1, transaction2)) {
                    duplicates.add(transaction1);
                    duplicates.add(transaction2);
                }
            }
        }

        return duplicates;
    }

    private static boolean isDuplicate(BankTransaction transaction1, BankTransaction transaction2) {
        if (transaction1 == transaction2) {
            return false;
        }

        if (!Objects.equals(transaction1.amount, transaction2.amount)) {
            return false;
        }

        if (!Objects.equals(transaction1.transactionType, transaction2.transactionType)) {
            return false;
        }

        long days = Duration.between(transaction1.transactionDate.atStartOfDay(), transaction2.transactionDate.atStartOfDay()).abs().toDays();
        return days < 5;
    }

    public boolean add(BankTransaction bankTransaction) {
        if (bankTransaction.id == null) {
            bankTransaction.id = UUID.randomUUID();
        }

        String query = """
                INSERT INTO bank_transactions (
                    id,
                    csv,
                    account,
                    transactionType,
                    transactionDate,
                    postDate,
                    description,
                    comments,
                    checkNumber,
                    amount,
                    balance
                )
                VALUES (
                    :id,
                    :csv,
                    :account,
                    :transactionType,
                    :transactionDate,
                    :postDate,
                    :description,
                    :comments,
                    :checkNumber,
                    :amount,
                    :balance
                )
                ON CONFLICT (id)
                DO UPDATE SET
                    id=:id,
                    csv=:csv,
                    account=:account,
                    transactionType=:transactionType,
                    transactionDate=:transactionDate,
                    postDate=:postDate,
                    description=:description,
                    comments=:comments,
                    checkNumber=:checkNumber,
                    amount=:amount,
                    balance=:balance
                """;

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", bankTransaction.id)
                .addValue("csv", bankTransaction.csv, Types.VARCHAR)
                .addValue("account", bankTransaction.account, Types.VARCHAR)
                .addValue("transactionType", bankTransaction.transactionType, Types.VARCHAR)
                .addValue("transactionDate", bankTransaction.transactionDate, Types.DATE)
                .addValue("postDate", bankTransaction.postDate, Types.DATE)
                .addValue("description", bankTransaction.description, Types.VARCHAR)
                .addValue("comments", bankTransaction.comments, Types.VARCHAR)
                .addValue("checkNumber", bankTransaction.checkNumber, Types.VARCHAR)
                .addValue("amount", Tools.toDatabaseInt(bankTransaction.amount), Types.INTEGER)
                .addValue("balance", Tools.toDatabaseInt(bankTransaction.balance), Types.INTEGER);

        return namedParameterJdbcTemplate.update(query, sqlParameterSource) == 1;
    }

    private static BankTransaction map(ResultSet resultSet, int i) throws SQLException {
        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.id = UUID.fromString(resultSet.getString("id"));
        bankTransaction.csv = resultSet.getString("csv");
        bankTransaction.account = resultSet.getString("account");
        bankTransaction.transactionDate = resultSet.getDate("transactionDate").toLocalDate();
        bankTransaction.transactionType = resultSet.getString("transactionType");
        Date postDate = resultSet.getDate("postDate");
        if (postDate != null) {
            bankTransaction.postDate = postDate.toLocalDate();
        }
        bankTransaction.description = resultSet.getString("description");
        bankTransaction.comments = resultSet.getString("comments");
        bankTransaction.checkNumber = resultSet.getString("checkNumber");
        bankTransaction.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));
        bankTransaction.balance = Tools.fromDatabaseInt(resultSet.getInt("balance"));
        return bankTransaction;
    }

}
