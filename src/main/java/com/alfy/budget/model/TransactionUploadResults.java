package com.alfy.budget.model;

public record TransactionUploadResults(
        int newTransactions,
        int existingTransactions,
        int failedTransactions,
        int autoCategorizedTransactions
) {

    @Override
    public String toString() {
        return "Transactions Added: " + newTransactions
                + " Existing Transactions: " + existingTransactions
                + " Failed Transactions: " + failedTransactions;
    }
}
