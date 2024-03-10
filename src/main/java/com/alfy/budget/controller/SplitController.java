package com.alfy.budget.controller;

import com.alfy.budget.model.Split;
import com.alfy.budget.model.SplitTransaction;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/rest/split")
public class SplitController {

    private final TransactionsService transactionsService;
    private final BankTransactionsService bankTransactionsService;

    public SplitController(
            TransactionsService transactionsService,
            BankTransactionsService bankTransactionsService
    ) {
        this.transactionsService = transactionsService;
        this.bankTransactionsService = bankTransactionsService;
    }

    @GetMapping(path = "/{bankTransactionId}")
    public Split getSplit(
            @PathVariable(name = "bankTransactionId") UUID bankTransactionId
    ) {
        Split split = new Split();
        split.bankTransaction = bankTransactionsService.get(bankTransactionId);
        split.transactions = transactionsService.listWithBankTransactionId(bankTransactionId).stream()
                .map(transaction -> {
                    SplitTransaction splitTransaction = new SplitTransaction();
                    splitTransaction.id = transaction.id;
                    splitTransaction.index = transaction.splitIndex;
                    splitTransaction.description = transaction.description;
                    splitTransaction.categoryId = transaction.category == null ? null : transaction.category.id;
                    splitTransaction.amount = Math.abs(transaction.amount) / 100d;
                    return splitTransaction;
                })
                .sorted(Comparator.comparing(transaction -> transaction.index))
                .collect(Collectors.toList());
        return split;
    }

    @PostMapping(path = "/{bankTransactionId}")
    public Split saveSplit(
            @PathVariable(name = "bankTransactionId") UUID bankTransactionId,
            @RequestBody Split split
    ) {
        if (split != null && split.transactions != null) {
            Set<UUID> idsToDelete = transactionsService.listWithBankTransactionId(bankTransactionId).stream()
                    .map(transaction -> transaction.id)
                    .collect(Collectors.toSet());

            List<SplitTransaction> transactions = split.transactions;
            for (int i = 0; i < transactions.size(); i++) {
                SplitTransaction transaction = transactions.get(i);
                transaction.index = i + 1;
                if (transaction.id == null) {
                    transactionsService.addSplitTransaction(split.bankTransaction, transaction);
                } else {
                    idsToDelete.remove(transaction.id);
                    transactionsService.updateTransaction(transaction);
                }
            }

            for (UUID transactionId : idsToDelete) {
                transactionsService.delete(transactionId);
            }
        }

        return getSplit(bankTransactionId);
    }
}
