package com.alfy.budget.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class BankTransaction {

    public UUID id;
    public String csv;
    public String account;
    public String transactionType;
    public LocalDate transactionDate;
    public LocalDate postDate;
    public String referenceNumber;
    public String description;
    public String comments;
    public String checkNumber;
    public BigDecimal amount;
    public BigDecimal balance;

}
