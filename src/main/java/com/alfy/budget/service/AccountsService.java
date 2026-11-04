package com.alfy.budget.service;

import com.alfy.budget.model.Account;
import com.alfy.budget.model.AccountEntry;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.tools.Tools;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Accounts are running totals over all time: credits in the account's categories add,
 * debits subtract.
 */
public class AccountsService {

    // Balance in cents, summed in SQL so the accounts list is one query.
    private static final String SELECT_WITH_BALANCE = """
            SELECT accounts.id,
                   accounts.name,
                   (SELECT count(*) FROM categories WHERE categories.account_id = accounts.id) AS category_count,
                   (SELECT coalesce(sum(CASE WHEN transactions.transaction_type = 'credit'
                                             THEN transactions.amount ELSE -transactions.amount END), 0)
                    FROM transactions
                    JOIN categories ON categories.id = transactions.category_id
                    WHERE categories.account_id = accounts.id) AS balance
            FROM accounts
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionsService transactionsService;

    public AccountsService(NamedParameterJdbcTemplate jdbc, TransactionsService transactionsService) {
        this.jdbc = jdbc;
        this.transactionsService = transactionsService;
    }

    public List<Account> list() {
        return jdbc.query(SELECT_WITH_BALANCE + " ORDER BY accounts.name", AccountsService::map);
    }

    public Account get(UUID id) {
        return jdbc.queryForObject(SELECT_WITH_BALANCE + " WHERE accounts.id = :id",
                new MapSqlParameterSource("id", Db.id(id)), AccountsService::map);
    }

    public UUID add(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, name) VALUES (:id, :name)",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(id))
                        .addValue("name", name.strip()));
        return id;
    }

    public void setName(UUID id, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        jdbc.update("UPDATE accounts SET name = :name WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", Db.id(id))
                        .addValue("name", name.strip()));
    }

    /** Its categories are left alone, they just stop counting toward any account. */
    public void delete(UUID id) {
        jdbc.update("DELETE FROM accounts WHERE id = :id", new MapSqlParameterSource("id", Db.id(id)));
    }

    /**
     * Makes exactly these categories count toward the account. A category that was in a
     * different account moves to this one.
     */
    public void setCategories(UUID accountId, Collection<UUID> categoryIds) {
        jdbc.update("UPDATE categories SET account_id = NULL WHERE account_id = :accountId",
                new MapSqlParameterSource("accountId", Db.id(accountId)));

        for (UUID categoryId : categoryIds) {
            jdbc.update("UPDATE categories SET account_id = :accountId WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("id", Db.id(categoryId))
                            .addValue("accountId", Db.id(accountId)));
        }
    }

    /** Newest first, each with the balance right after that transaction. */
    public List<AccountEntry> history(UUID accountId) {
        List<AccountEntry> entries = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;

        for (Transaction transaction : transactionsService.listForAccount(accountId)) {
            balance = transaction.credit() ? balance.add(transaction.amount) : balance.subtract(transaction.amount);
            entries.add(new AccountEntry(transaction, balance));
        }

        Collections.reverse(entries);
        return entries;
    }

    private static Account map(ResultSet resultSet, int rowNum) throws SQLException {
        Account account = new Account();
        account.id = Db.getId(resultSet, "id");
        account.name = resultSet.getString("name");
        account.categoryCount = resultSet.getInt("category_count");
        account.balance = Tools.fromDatabaseInt(resultSet.getInt("balance"));
        return account;
    }
}
