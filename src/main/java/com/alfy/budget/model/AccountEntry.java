package com.alfy.budget.model;

import java.math.BigDecimal;

/**
 * One transaction in an account's history, with the account balance right after it.
 */
public record AccountEntry(Transaction transaction, BigDecimal balanceAfter) {
}
