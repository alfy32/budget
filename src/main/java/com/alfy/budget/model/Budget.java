package com.alfy.budget.model;

import java.util.UUID;

public class Budget {

    public UUID id;
    public String name;
    public double amount;

    public static Budget create(String name, double amount) {
        Budget budget = new Budget();
        budget.name = name;
        budget.amount = amount;
        return budget;
    }

}
