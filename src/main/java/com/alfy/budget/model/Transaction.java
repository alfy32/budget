package com.alfy.budget.model;

import java.time.LocalDate;
import java.util.UUID;

public class Transaction {

    public UUID id;
    public BankTransaction bankTransaction;
    public int splitIndex;
    public String account;
    public String transactionType;
    public LocalDate transactionDate;
    public String description;
    public double amount;
    public Category category;
    public String tags;
    public String notes;

    public boolean credit() {
        return "credit".equalsIgnoreCase(transactionType);
    }

    public boolean debit() {
        return "debit".equalsIgnoreCase(transactionType);
    }

}
