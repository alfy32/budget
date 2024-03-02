package com.alfy.budget.model;

import java.time.LocalDate;
import java.util.UUID;

public class BankTransaction {

    public UUID id;
    public String csv;
    public String account;
    public LocalDate transactionDate;
    public String description;
    public String comments;
    public String checkNumber;
    public int amount;

}
