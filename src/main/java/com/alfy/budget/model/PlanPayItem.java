package com.alfy.budget.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * One line in a plan's pay: a fixed amount per year, or a percent of gross (or taxable) salary.
 */
public class PlanPayItem {

    public UUID id;
    public PaySection section;
    public String name;
    /** true: value is a percent (e.g. 6.2), false: value is dollars per year. */
    public boolean percent;
    /** For a percent: of taxable salary instead of gross. */
    public boolean ofTaxable;
    public BigDecimal value = BigDecimal.ZERO;
    /** Counts toward the retirement total. */
    public boolean retirement;
    /** Counts toward the HSA total (checked against the plan's HSA limit). */
    public boolean hsa;

    /** Dollars per year. */
    public BigDecimal amount(BigDecimal gross, BigDecimal taxable) {
        if (!percent) {
            return value;
        }
        return (ofTaxable ? taxable : gross).multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

}
