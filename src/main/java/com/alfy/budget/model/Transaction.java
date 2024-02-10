package com.alfy.budget.model;

import java.text.NumberFormat;
import java.time.LocalDate;

public class Transaction {

    public int id;
    public int bankTransactionId;
    public String account;
    public LocalDate date;
    public String description;
    public String comments;
    public String checkNumber;
    public int amount;
    public String category;
    public String tags;
    public String notes;


    public String getFormattedAmount() {
        return NumberFormat.getCurrencyInstance().format(amount / 100d);
    }

}
