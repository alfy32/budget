package com.alfy.budget.controller;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Category;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/rest/transactions")
public class TransactionsController {

    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;
    private final BankTransactionsService bankTransactionsService;

    public TransactionsController(
            CategoriesService categoriesService,
            TransactionsService transactionsService,
            BankTransactionsService bankTransactionsService
    ) {
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
        this.bankTransactionsService = bankTransactionsService;
    }

    @GetMapping
    public List<Transaction> getTransactions(
            @RequestParam(name = "needsCategorized", required = false) boolean needsCategorized,
            @RequestParam(name = "needsTransferred", required = false) boolean needsTransferred
    ) {
        List<Transaction> transactionsOrderedByDate = transactionsService.listOrderedByDate(needsCategorized, needsTransferred);

        if (needsCategorized) {
            transactionsOrderedByDate.removeIf(transaction -> transaction.category != null);
        }

        if (needsTransferred) {
            transactionsOrderedByDate.removeIf(transaction -> !transaction.needsTransferred);
        }

        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        for (Transaction transaction : transactionsOrderedByDate) {
            if (transaction.category != null && transaction.category.id != null) {
                transaction.category = categoriesById.get(transaction.category.id);
            }
        }

        return transactionsOrderedByDate;
    }

    @PostMapping(path = "/create")
    public UUID createTransaction(
            @RequestParam(name= "account", defaultValue = "Cash") String account,
            @RequestParam(name= "transactionType", defaultValue = "debit") String transactionType
    ) {
        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.id = UUID.randomUUID();
        bankTransaction.csv = "UserTransaction" + bankTransaction.id;
        bankTransaction.account = account;
        bankTransaction.transactionType = transactionType;
        bankTransaction.transactionDate = LocalDate.now();
        bankTransaction.postDate = LocalDate.now();
        bankTransaction.description = "New Transaction";
        bankTransaction.amount = new BigDecimal("1.00");
        bankTransactionsService.add(bankTransaction);
        return transactionsService.addFrom(bankTransaction);
    }

    @GetMapping(path = "/{id}")
    public Transaction getTransaction(
            @PathVariable(name = "id") UUID id
    ) {
        Transaction transaction = transactionsService.get(id);
        if (transaction.category != null && transaction.category.id != null) {
            transaction.category = categoriesService.get(transaction.category.id);
        }

        if (transaction.bankTransaction != null && transaction.bankTransaction.id != null) {
            transaction.bankTransaction = bankTransactionsService.get(transaction.bankTransaction.id);
        }

        return transaction;
    }

    @GetMapping(path = "/{id}/description")
    public Map<String, Object> getTransactionDescription(
            @PathVariable(name = "id") UUID id
    ) {
        return transactionsService.getTransactionDescription(id);
    }

    @PostMapping(path = "/{id}/amount")
    public void updateAmount(
            @PathVariable(name = "id") UUID id,
            @RequestBody BigDecimal amount
    ) {
        transactionsService.updateAmount(id, amount);
    }

    @PostMapping(path = "/{id}/description")
    public void updateTransactionDescription(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "description") String description
    ) {
        transactionsService.updateDescription(id, description);
    }

    @PostMapping(path = "/{id}/category")
    public void updateTransactionCategory(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "categoryId") UUID categoryId
    ) {
        transactionsService.updateCategory(id, categoryId);
    }

    @PostMapping(path = "/{id}/tags")
    public void updateTags(
            @PathVariable(name = "id") UUID transactionId,
            @RequestBody(required = false) List<String> tags
    ) {
        String tagsValue = tags == null ? null : String.join(", ", tags);
        transactionsService.updateTags(transactionId, tagsValue);
    }

    @PostMapping(path = "/{id}/notes")
    public void updateNotes(
            @PathVariable("id") UUID transactionId,
            @RequestBody(required = false) String note
    ) {
        if (note == null || note.isEmpty()) {
            note = null;
        }

        transactionsService.updateNotes(transactionId, note);
    }

    @PostMapping(path = "/{id}/date")
    public void updateDate(
            @PathVariable("id") UUID transactionId,
            @RequestBody LocalDate transactionDate
    ) {
        transactionsService.updateDate(transactionId, transactionDate);
    }

    @PostMapping(path = "/{id}/type")
    public void updateType(
            @PathVariable("id") UUID transactionId,
            @RequestBody(required = false) String transactionType
    ) {
        if ("credit".equals(transactionType) || "debit".equals(transactionType)) {
            transactionsService.updateType(transactionId, transactionType);
        }
    }

    @PutMapping(path = "/{id}/needsTransferred")
    public void needsTransferred(
            @PathVariable("id") UUID transactionId
    ) {
        transactionsService.needsTransferred(transactionId, true);
    }

    @PutMapping(path = "/{id}/transferComplete")
    public void transferComplete(
            @PathVariable("id") UUID transactionId
    ) {
        transactionsService.needsTransferred(transactionId, false);
    }

}
