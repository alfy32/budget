package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.List;

public class BudgetInfo {

    public BigDecimal total;
    public BigDecimal expectedTotal;
    public int percent;
    public int expectedPercent;
    public Budget budget;
    /** Which part of the plan the budget is in (for grouping the yearly page). */
    public PlanGroup group = PlanGroup.MONTHLY;
    public List<CategoryInfo> categories;

}
