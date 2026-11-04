package com.alfy.budget.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A budget plan: the set-up agreed on at a planning sit-down, in effect from startMonth until the next plan
 * starts (end is null for the newest plan). A plan with no startMonth is a proposal that nothing uses yet.
 */
public class Plan {

    public UUID id;
    public LocalDate startMonth;
    /** The day before the next plan starts, or null if this is the newest plan. */
    public LocalDate end;
    /** Take-home pay per month. */
    public BigDecimal takeHome = BigDecimal.ZERO;
    /** The savings per month to stay at or above, or null. */
    public BigDecimal savingsTarget;
    public String notes;

    public boolean isProposed() {
        return startMonth == null;
    }

}
