package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/rest/transactions")
public class TransactionsController {

    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;

    public TransactionsController(
            CategoriesService categoriesService,
            TransactionsService transactionsService
    ) {
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public List<Transaction> getTransactions(
            @RequestParam(name = "needsCategorized", required = false) boolean needsCategorized
    ) {
        List<Transaction> transactionsOrderedByDate = transactionsService.getTransactionsOrderedByDate();
        if (needsCategorized) {
            transactionsOrderedByDate.removeIf(transaction -> transaction.category != null);
        }

        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        for (Transaction transaction : transactionsOrderedByDate) {
            if (transaction.category != null && transaction.category.id != null) {
                transaction.category = categoriesById.get(transaction.category.id);
            }
        }

        return transactionsOrderedByDate;
    }

    @GetMapping(path = "/{id}")
    public Transaction getTransaction(
            @PathVariable(name = "id") UUID id
    ) {
        Transaction transaction = transactionsService.get(id);
        if (transaction.category != null && transaction.category.id != null) {
            transaction.category = categoriesService.get(transaction.category.id);
        }
        return transaction;
    }

    @GetMapping(path = "/{id}/description")
    public Map<String, Object> getTransactionDescription(
            @PathVariable(name = "id") UUID id
    ) {
        return transactionsService.getTransactionDescription(id);
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
            @RequestBody List<String> tags
    ) {
        String tagsValue = tags == null ? null : String.join(", ", tags);
        transactionsService.updateTags(transactionId, tagsValue);
    }

    @PostMapping(path = "/{id}/notes")
    public void update(
            @PathVariable("id") UUID transactionId,
            @RequestBody String note
    ) {
        if (note == null || note.isEmpty()) {
            note = null;
        }

        transactionsService.updateNotes(transactionId, note);
    }

}
