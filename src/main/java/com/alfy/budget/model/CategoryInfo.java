package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.List;

public class CategoryInfo {

    public Category category;
    public BigDecimal total;
    public List<Transaction> transactions;

    public CategoryInfo() {
        this.total = BigDecimal.ZERO;
    }

}
