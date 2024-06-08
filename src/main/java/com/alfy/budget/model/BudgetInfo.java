package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.List;

public class BudgetInfo {

    public BigDecimal total;
    public BigDecimal expectedTotal;
    public int percent;
    public int expectedPercent;
    public Budget budget;
    public List<CategoryInfo> categories;

}
