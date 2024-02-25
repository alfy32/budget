package com.alfy.budget.controller;

import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/rest/transactions")
public class TransactionsController {

    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService transactionsService) {
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
        return transactionsOrderedByDate;
    }

    @GetMapping(path = "/{id}")
    public Transaction getTransaction(
            @PathVariable(name = "id") int id
    ) {
        return transactionsService.getTransaction(id);
    }

    @GetMapping(path = "/{id}/description")
    public Map<String, Object> getTransactionDescription(
            @PathVariable(name = "id") int id
    ) {
        return transactionsService.getTransactionDescription(id);
    }

    @PostMapping(path = "/{id}/description")
    public void updateTransactionDescription(
            @PathVariable(name = "id") int id,
            @RequestParam(name = "description") String description
    ) {
        transactionsService.updateDescription(id, description);
    }

    @PostMapping(path = "/{id}/category")
    public void updateTransactionCategory(
            @PathVariable(name = "id") int id,
            @RequestParam(name = "category") String category
    ) {
        transactionsService.updateCategory(id, category);
    }

    @PostMapping(path = "/{id}/tags")
    public void updateTags(
            @PathVariable(name = "id") int transactionId,
            @RequestBody List<String> tags
    ) {
        String tagsValue = tags == null ? null : String.join(", ", tags);
        transactionsService.updateTags(transactionId, tagsValue);
    }

    @PostMapping(path = "/{id}/notes")
    public void update(
            @PathVariable("id") int transactionId,
            @RequestBody String note
    ) {
        if (note == null || note.isEmpty()) {
            note = null;
        }

        transactionsService.updateNotes(transactionId, note);
    }

}
