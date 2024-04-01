package com.alfy.budget.model;

import java.time.LocalDate;
import java.util.UUID;

public class Transaction {

    public UUID id;
    public BankTransaction bankTransaction;
    public int splitIndex;
    public String account;
    public LocalDate transactionDate;
    public String description;
    public int amount;
    public Category category;
    public String tags;
    public String notes;

    public double getAmount() {
        return amount / 100d;
    }

}
