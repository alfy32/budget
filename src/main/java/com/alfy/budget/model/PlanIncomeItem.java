package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Money a plan sets aside from income that isn't a spending category, e.g. "Alan Roth IRA".
 */
public class PlanIncomeItem {

    public UUID id;
    public String name;
    /** Per year. */
    public BigDecimal amount = BigDecimal.ZERO;

}
