package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.List;

public class CategoryInfo {

    public Category category;
    public BigDecimal total;
    /** What the plan(s) gave this category for the period, or null if it isn't in a plan. */
    public BigDecimal amount;
    public List<Transaction> transactions;

    public CategoryInfo() {
        this.total = BigDecimal.ZERO;
    }

}
