package com.alfy.budget.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * One category in a plan: which budget it belongs to and how much it gets per year.
 */
public class PlanLine {

    public UUID planId;
    public UUID categoryId;
    public String categoryName;
    public UUID budgetId;
    public String budgetName;
    public PlanGroup group;
    /** Per year. */
    public BigDecimal amount = BigDecimal.ZERO;
    /** The unit the amount is entered in on the worksheet: year, month or paycheck. */
    public String unit = "year";

    public BigDecimal perMonth() {
        return amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

}
