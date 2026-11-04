package com.alfy.budget.model;

import java.util.List;

/**
 * The bank and cash accounts transactions come from (stored as text on each transaction).
 * Not to be confused with Account, the running totals on the Accounts page.
 *
 * To add one: put its name here. If it can be uploaded, also add it to UPLOADABLE and give it a case in
 * TransactionsUploadController.parse that reads its CSV format.
 * Old transactions keep whatever account name they were saved with, even if it's no longer listed.
 */
public final class BankAccounts {

    /** Offered when adding a transaction by hand; the first one is picked by default. */
    public static final List<String> ALL = List.of(
            "Cash",
            "SBSU Checking",
            "Zions Agility Cash Visa",
            "Zions Amazing Cash Visa"
    );

    /** The ones that have a bank CSV export you can upload; the first one is picked by default. */
    public static final List<String> UPLOADABLE = List.of(
            "SBSU Checking",
            "Zions Agility Cash Visa",
            "Zions Amazing Cash Visa"
    );

    private BankAccounts() {
    }

}
