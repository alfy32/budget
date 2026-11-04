package com.alfy.budget.model;

/**
 * The parts of a plan's pay, in the order the paycheck is worked out (see V6__plan_pay.sql).
 */
public enum PaySection {
    PRETAX("pretax", "Before tax", "Taken out before tax, e.g. dental, HSA, accident plan"),
    TAX("tax", "Taxes", "Federal (type it in; fix it after the first paycheck), social security, medicare, state"),
    POSTTAX("posttax", "After tax", "Taken out of the paycheck after tax, e.g. Roth 401k"),
    TRANSFER("transfer", "Through checking", "Deposited to checking, then moved out (e.g. Roth IRA, missionaries)"),
    DONATION("donation", "Donations", "Paid from income every month, e.g. tithing as 10% of gross"),
    INFO("info", "From the employer", "For reference; doesn't change the paycheck (e.g. HSA contribution, 401k match)"),
    INSURANCE("insurance", "Insurance (emergency fund)", "What insurance would cost without the job; counts toward the emergency fund");

    public final String dbValue;
    public final String label;
    public final String description;

    PaySection(String dbValue, String label, String description) {
        this.dbValue = dbValue;
        this.label = label;
        this.description = description;
    }

    public static PaySection fromDb(String value) {
        for (PaySection section : values()) {
            if (section.dbValue.equals(value)) {
                return section;
            }
        }
        return INFO;
    }
}
