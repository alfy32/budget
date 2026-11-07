package com.alfy.budget.model;

/**
 * Which part of the plan a budget belongs to. Savings per month = take-home pay - from income - spending
 * (monthly + yearly). "Other" budgets are paid from somewhere else and aren't counted.
 */
public enum PlanGroup {
    MONTHLY("monthly", "Monthly", "Regular spending out of checking"),
    YEARLY("yearly", "Yearly", "Irregular spending, saved monthly in Yearly Savings"),
    INCOME("income", "From income", "Only paid when there's income, like donations; not part of spending"),
    OTHER("other", "Other", "Paid from somewhere else; not counted against savings");

    public final String dbValue;
    public final String label;
    public final String description;

    PlanGroup(String dbValue, String label, String description) {
        this.dbValue = dbValue;
        this.label = label;
        this.description = description;
    }

    /** Monthly and yearly budgets are what you spend; from-income and other aren't. */
    public boolean isSpending() {
        return this == MONTHLY || this == YEARLY;
    }

    public static PlanGroup fromDb(String value) {
        for (PlanGroup group : values()) {
            if (group.dbValue.equals(value)) {
                return group;
            }
        }
        return MONTHLY;
    }
}
