package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {

    public UUID id;
    public String name;
    /** Credits minus debits of every transaction in the account's categories. */
    public BigDecimal balance;
    public int categoryCount;

}
