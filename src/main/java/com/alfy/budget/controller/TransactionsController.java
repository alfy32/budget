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
import java.util.Collections;
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
    public List<Transaction> queryTransactions(
            @RequestParam(name = "query", required = false) String query
    ) {
        List<Transaction> transactionsOrderedByDate = getTransactions(query);

        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        for (Transaction transaction : transactionsOrderedByDate) {
            if (transaction.category != null && transaction.category.id != null) {
                transaction.category = categoriesById.get(transaction.category.id);
            }
        }

        return transactionsOrderedByDate;
    }

    private List<Transaction> getTransactions(String query) {
        if (query != null ) {
            if ("needsCategorized".equals(query)) {
                return transactionsService.listOrderedByDate(
                        true,
                        false
                );
            } else if ("needsTransferred".equals(query)) {
                return transactionsService.listOrderedByDate(
                        false,
                        true
                );
            } else if (query.startsWith("category,")) {
                return getCategoryTransactions(query);
            }
        }

        return transactionsService.listOrderedByDate(
                false,
                false
        );
    }

    private List<Transaction> getCategoryTransactions(String query) {
        String[] split = query.split(",");
        if ("category".equals(split[0])) {
            UUID categoryId = UUID.fromString(split[1]);
            LocalDate startDate = LocalDate.parse(split[2]);
            LocalDate endDate = LocalDate.parse(split[3]);
            return transactionsService.listOrderedByDateWithCategory(categoryId, startDate, endDate);
        }

        return Collections.emptyList();
    }

    @PostMapping(path = "/create")
    public UUID createTransaction(
            @RequestBody BankTransaction bankTransaction
    ) {
        if (bankTransaction.account == null || bankTransaction.account.isEmpty()) {
            throw new IllegalArgumentException("'account' is required");
        }

        if (!"credit".equals(bankTransaction.transactionType) && !"debit".equals(bankTransaction.transactionType)) {
            throw new IllegalArgumentException("'transactionType' of 'credit' or 'debit' required");
        }

        if (bankTransaction.transactionDate == null) {
            throw new IllegalArgumentException("'transactionDate' is required");
        }

        if (bankTransaction.description == null || bankTransaction.description.isEmpty()) {
            throw new IllegalArgumentException("'description' is required");
        }

        if (bankTransaction.amount == null) {
            throw new IllegalArgumentException("'amount' is required");
        }

        bankTransaction.id = UUID.randomUUID();
        bankTransaction.csv = "UserTransaction" + bankTransaction.id;
        bankTransaction.postDate = bankTransaction.transactionDate;
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
