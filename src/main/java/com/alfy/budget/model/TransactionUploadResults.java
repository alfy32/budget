package com.alfy.budget.model;

public class TransactionUploadResults {

    private final int newTransactions;
    private final int existingTransactions;

    public TransactionUploadResults(
            int newTransactions,
            int existingTransactions
    ) {
        this.newTransactions = newTransactions;
        this.existingTransactions = existingTransactions;
    }

    public int getNewTransactions() {
        return newTransactions;
    }

    public int getExistingTransactions() {
        return existingTransactions;
    }

    @Override
    public String toString() {
        return "Transactions Added: " + newTransactions
                + " Existing Transactions: " + existingTransactions;
    }
}
