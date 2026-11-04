package com.alfy.budget.controller;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.SplitTransaction;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Splits one bank transaction into several transactions, each with its own category and amount.
 * The first row always gets whatever is left over, so the rows add up to the bank amount.
 */
@Controller
public class SplitController {

    private final TransactionsService transactionsService;
    private final BankTransactionsService bankTransactionsService;
    private final CategoriesService categoriesService;

    public SplitController(
            TransactionsService transactionsService,
            BankTransactionsService bankTransactionsService,
            CategoriesService categoriesService
    ) {
        this.transactionsService = transactionsService;
        this.bankTransactionsService = bankTransactionsService;
        this.categoriesService = categoriesService;
    }

    @GetMapping("/split/{bankTransactionId}")
    public String split(
            @PathVariable UUID bankTransactionId,
            @RequestParam(required = false) String query,
            Model model
    ) {
        BankTransaction bankTransaction = bankTransactionsService.get(bankTransactionId);

        // Plain strings so the page's JavaScript can read them directly.
        List<Map<String, String>> rows = new ArrayList<>();
        for (Transaction transaction : transactionsService.listWithBankTransactionId(bankTransactionId)) {
            Map<String, String> row = new HashMap<>();
            row.put("id", transaction.id.toString());
            row.put("categoryId", transaction.category == null ? "" : transaction.category.id.toString());
            row.put("amount", transaction.amount.toPlainString());
            rows.add(row);
        }

        model.addAttribute("bankTransaction", bankTransaction);
        model.addAttribute("rows", rows);
        model.addAttribute("categories", categoriesService.list());
        model.addAttribute("query", query);
        return "split";
    }

    @PostMapping("/split/{bankTransactionId}")
    public String save(
            @PathVariable UUID bankTransactionId,
            @ModelAttribute SplitForm form,
            @RequestParam(required = false) String query
    ) {
        BankTransaction bankTransaction = bankTransactionsService.get(bankTransactionId);
        List<Transaction> existing = transactionsService.listWithBankTransactionId(bankTransactionId);
        List<SplitForm.Row> rows = form.getRows().stream().filter(Objects::nonNull).toList();
        if (rows.isEmpty() || existing.isEmpty()) {
            return TransactionsController.redirectToTransaction(existing.getFirst().id, query);
        }

        // New rows get the description of the first row, like the old app did.
        Transaction first = existing.stream()
                .filter(transaction -> rows.getFirst().id != null && transaction.id.equals(rows.getFirst().id))
                .findFirst()
                .orElse(existing.getFirst());

        // Don't trust the browser's math for the first row.
        BigDecimal rest = BigDecimal.ZERO;
        for (int i = 1; i < rows.size(); i++) {
            rest = rest.add(rows.get(i).amount == null ? BigDecimal.ZERO : rows.get(i).amount);
        }
        rows.getFirst().amount = bankTransaction.amount.subtract(rest);

        Set<UUID> idsToDelete = new HashSet<>();
        for (Transaction transaction : existing) {
            idsToDelete.add(transaction.id);
        }

        UUID firstId = first.id;
        for (int i = 0; i < rows.size(); i++) {
            SplitForm.Row row = rows.get(i);

            SplitTransaction splitTransaction = new SplitTransaction();
            splitTransaction.id = row.id;
            splitTransaction.index = rows.size() == 1 ? -1 : i + 1;
            splitTransaction.categoryId = row.categoryId;
            splitTransaction.amount = row.amount == null ? BigDecimal.ZERO : row.amount;

            if (row.id != null && idsToDelete.remove(row.id)) {
                Transaction current = existing.stream().filter(t -> t.id.equals(row.id)).findFirst().orElseThrow();
                splitTransaction.description = current.description;
                transactionsService.updateTransaction(splitTransaction);
            } else {
                splitTransaction.description = first.description;
                transactionsService.addSplitTransaction(bankTransaction, splitTransaction, first.transferredFrom);
            }
        }

        idsToDelete.remove(firstId);
        for (UUID id : idsToDelete) {
            transactionsService.delete(id);
        }

        return TransactionsController.redirectToTransaction(firstId, query);
    }

    public static class SplitForm {
        private List<Row> rows = new ArrayList<>();

        public List<Row> getRows() {
            return rows;
        }

        public void setRows(List<Row> rows) {
            this.rows = rows;
        }

        public static class Row {
            public UUID id;
            public UUID categoryId;
            public BigDecimal amount;

            public void setId(UUID id) {
                this.id = id;
            }

            public void setCategoryId(UUID categoryId) {
                this.categoryId = categoryId;
            }

            public void setAmount(BigDecimal amount) {
                this.amount = amount;
            }
        }
    }
}
