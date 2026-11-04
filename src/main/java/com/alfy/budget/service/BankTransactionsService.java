package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.PossibleDuplicateTransactions;
import com.alfy.budget.tools.Tools;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BankTransactionsService {

    private final NamedParameterJdbcTemplate jdbc;

    public BankTransactionsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsReferenceNumber(String account, String referenceNumber) {
        if (isBlank(account) || isBlank(referenceNumber)) {
            return false;
        }

        return exists("""
                SELECT 1 FROM bank_transactions
                WHERE account = :account
                  AND reference_number = :referenceNumber
                """, new MapSqlParameterSource()
                .addValue("account", account)
                .addValue("referenceNumber", referenceNumber));
    }

    public boolean exists(String account, String csv) {
        if (isBlank(account) || isBlank(csv)) {
            return false;
        }

        return exists("""
                SELECT 1 FROM bank_transactions
                WHERE account = :account
                  AND csv = :csv
                """, new MapSqlParameterSource()
                .addValue("account", account)
                .addValue("csv", csv));
    }

    public boolean exists(String account, BankTransaction bankTransaction) {
        if (isBlank(account) || bankTransaction == null) {
            return false;
        }

        return exists("""
                SELECT 1 FROM bank_transactions
                WHERE account = :account
                  AND transaction_type = :transactionType
                  AND description = :description
                  AND amount = :amount
                  AND balance = :balance
                """, new MapSqlParameterSource()
                .addValue("account", bankTransaction.account)
                .addValue("transactionType", bankTransaction.transactionType)
                .addValue("description", bankTransaction.description)
                .addValue("amount", Tools.toDatabaseInt(bankTransaction.amount))
                .addValue("balance", Tools.toDatabaseInt(bankTransaction.balance)));
    }

    private boolean exists(String query, MapSqlParameterSource params) {
        return !jdbc.queryForList(query, params).isEmpty();
    }

    public BankTransaction get(UUID id) {
        return jdbc.queryForObject("SELECT * FROM bank_transactions WHERE id = :id",
                new MapSqlParameterSource("id", Db.id(id)), BankTransactionsService::map);
    }

    public List<PossibleDuplicateTransactions> listPossibleDuplicates() {
        List<BankTransaction> transactions = jdbc.query(
                "SELECT * FROM bank_transactions WHERE transaction_date > :start",
                new MapSqlParameterSource("start", Db.date(LocalDate.now().minusMonths(12))),
                BankTransactionsService::map);

        List<PossibleDuplicateTransactions> possibleDuplicates = new ArrayList<>();
        for (int i = 0; i < transactions.size() - 1; i++) {
            BankTransaction transaction1 = transactions.get(i);
            for (int j = i + 1; j < transactions.size(); j++) {
                BankTransaction transaction2 = transactions.get(j);
                if (isDuplicate(transaction1, transaction2)) {
                    PossibleDuplicateTransactions possibleDuplicate = new PossibleDuplicateTransactions();
                    possibleDuplicate.transaction1 = transaction1;
                    possibleDuplicate.transaction2 = transaction2;
                    possibleDuplicates.add(possibleDuplicate);
                }
            }
        }

        return possibleDuplicates;
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

        long days = Math.abs(ChronoUnit.DAYS.between(transaction1.transactionDate, transaction2.transactionDate));
        return days < 5;
    }

    public boolean add(BankTransaction bankTransaction) {
        if (bankTransaction.id == null) {
            bankTransaction.id = UUID.randomUUID();
        }

        String query = """
                INSERT INTO bank_transactions (
                    id, csv, account, transaction_type, transaction_date, post_date, reference_number,
                    description, comments, check_number, amount, balance
                )
                VALUES (
                    :id, :csv, :account, :transactionType, :transactionDate, :postDate, :referenceNumber,
                    :description, :comments, :checkNumber, :amount, :balance
                )
                ON CONFLICT (id) DO UPDATE SET
                    csv = excluded.csv,
                    account = excluded.account,
                    transaction_type = excluded.transaction_type,
                    transaction_date = excluded.transaction_date,
                    post_date = excluded.post_date,
                    reference_number = excluded.reference_number,
                    description = excluded.description,
                    comments = excluded.comments,
                    check_number = excluded.check_number,
                    amount = excluded.amount,
                    balance = excluded.balance
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", Db.id(bankTransaction.id))
                .addValue("csv", bankTransaction.csv)
                .addValue("account", bankTransaction.account)
                .addValue("transactionType", bankTransaction.transactionType)
                .addValue("transactionDate", Db.date(bankTransaction.transactionDate))
                .addValue("postDate", Db.date(bankTransaction.postDate))
                .addValue("referenceNumber", bankTransaction.referenceNumber)
                .addValue("description", bankTransaction.description)
                .addValue("comments", bankTransaction.comments)
                .addValue("checkNumber", bankTransaction.checkNumber)
                .addValue("amount", Tools.toDatabaseInt(bankTransaction.amount))
                .addValue("balance", Tools.toDatabaseInt(bankTransaction.balance));

        return jdbc.update(query, params) == 1;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    private static BankTransaction map(ResultSet resultSet, int rowNum) throws SQLException {
        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.id = Db.getId(resultSet, "id");
        bankTransaction.csv = resultSet.getString("csv");
        bankTransaction.account = resultSet.getString("account");
        bankTransaction.transactionType = resultSet.getString("transaction_type");
        bankTransaction.transactionDate = Db.getDate(resultSet, "transaction_date");
        bankTransaction.postDate = Db.getDate(resultSet, "post_date");
        bankTransaction.referenceNumber = resultSet.getString("reference_number");
        bankTransaction.description = resultSet.getString("description");
        bankTransaction.comments = resultSet.getString("comments");
        bankTransaction.checkNumber = resultSet.getString("check_number");
        bankTransaction.amount = Tools.fromDatabaseInt(resultSet.getInt("amount"));
        bankTransaction.balance = Tools.fromDatabaseInt(resultSet.getInt("balance"));
        return bankTransaction;
    }

}
