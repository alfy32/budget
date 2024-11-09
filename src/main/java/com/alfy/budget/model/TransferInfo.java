package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.List;

public class TransferInfo {

    public String account;
    public BigDecimal amount;
    public List<BudgetInfo> budgets;

}
