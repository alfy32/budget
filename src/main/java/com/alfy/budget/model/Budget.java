package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Budget {

    public UUID id;
    public String name;
    public BigDecimal amount;
    public boolean monthly;
    public String transferAccount;
    /** Which part of the plan it belongs to. */
    public PlanGroup group = PlanGroup.MONTHLY;

    public static Budget create(String name, BigDecimal amount) {
        Budget budget = new Budget();
        budget.name = name;
        budget.amount = amount;
        return budget;
    }

}
