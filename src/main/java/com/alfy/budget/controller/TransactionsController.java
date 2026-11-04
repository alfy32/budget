package com.alfy.budget.controller;

import com.alfy.budget.model.BankAccounts;
import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Category;
import com.alfy.budget.model.Transaction;
import com.alfy.budget.model.TransactionFilter;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
public class TransactionsController {

    /** Tags you can put on a transaction. */
    static final List<String> TAGS = List.of("Medical", "Car", "Home Improvement", "Savings", "Gift");

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

    // ---- List --------------------------------------------------------------------------------

    /** How many transactions to load at a time as you scroll. */
    static final int PAGE_SIZE = 50;

    /**
     * query is one of: needsCategorized, needsTransferred,
     * category,{id},{start},{end} or category,{id},needsTransferred. Anything else lists everything.
     *
     * The first request renders the whole page with the newest PAGE_SIZE transactions. At the bottom is a
     * placeholder that htmx swaps for the next batch when it scrolls into view (page=1, 2, ...), which only
     * renders the "batch" fragment. prevDate is the date the previous batch ended on, so a day that spans
     * two batches doesn't get its heading twice.
     */
    @GetMapping("/transactions")
    public String list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prevDate,
            Model model
    ) {
        page = Math.max(page, 0);
        List<Transaction> transactions = new ArrayList<>(transactionsService.page(filter(query), search, PAGE_SIZE + 1, page * PAGE_SIZE));
        boolean hasMore = transactions.size() > PAGE_SIZE;
        if (hasMore) {
            transactions.removeLast();
        }

        Map<UUID, Category> categoriesById = categoriesService.getCategoriesById();
        for (Transaction transaction : transactions) {
            if (transaction.category != null) {
                transaction.category = categoriesById.get(transaction.category.id);
            }
        }

        model.addAttribute("days", groupByDay(transactions, prevDate));
        model.addAttribute("query", query);
        model.addAttribute("search", search);
        model.addAttribute("nextUrl", hasMore ? nextUrl(query, search, page + 1, transactions.getLast().transactionDate) : null);

        if (page > 0) {
            return "transactions :: batch";
        }

        model.addAttribute("title", title(query, categoriesById));
        model.addAttribute("tab", "needsCategorized".equals(query) ? "categorize" : "transactions");
        model.addAttribute("back", query != null && query.startsWith("category,") ? "javascript:history.back()" : null);
        return "transactions";
    }

    private static String nextUrl(String query, String search, int page, LocalDate prevDate) {
        return UriComponentsBuilder.fromPath("/transactions")
                .queryParamIfPresent("query", Optional.ofNullable(query))
                .queryParamIfPresent("search", Optional.ofNullable(search))
                .queryParam("page", page)
                .queryParam("prevDate", prevDate)
                .encode()
                .toUriString();
    }

    private static TransactionFilter filter(String query) {
        if (query == null) {
            return TransactionFilter.ALL;
        }
        if (query.equals("needsCategorized")) {
            return new TransactionFilter(true, false, null, null, null);
        }
        if (query.equals("needsTransferred")) {
            return new TransactionFilter(false, true, null, null, null);
        }
        if (query.startsWith("category,")) {
            String[] parts = query.split(",");
            UUID categoryId = UUID.fromString(parts[1]);
            if (parts[2].equals("needsTransferred")) {
                return new TransactionFilter(false, true, categoryId, null, null);
            }
            return new TransactionFilter(false, false, categoryId, LocalDate.parse(parts[2]), LocalDate.parse(parts[3]));
        }
        return TransactionFilter.ALL;
    }


    /**
     * Transactions that share a date, with a label like "Today" or "Thu, Oct 30".
     * continued means the day started in the previous batch, so its heading is already on screen.
     */
    public record Day(String label, List<Transaction> transactions, boolean continued) {
    }

    private static List<Day> groupByDay(List<Transaction> transactions, LocalDate prevDate) {
        Map<LocalDate, List<Transaction>> byDate = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            byDate.computeIfAbsent(transaction.transactionDate, date -> new ArrayList<>()).add(transaction);
        }

        LocalDate today = LocalDate.now();
        List<Day> days = new ArrayList<>();
        byDate.forEach((date, list) -> days.add(new Day(dayLabel(date, today), list, date.equals(prevDate))));
        return days;
    }

    static String dayLabel(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "Today";
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        String pattern = date.getYear() == today.getYear() ? "EEE, MMM d" : "EEE, MMM d, yyyy";
        return date.format(java.time.format.DateTimeFormatter.ofPattern(pattern, Locale.US));
    }

    private static String title(String query, Map<UUID, Category> categoriesById) {
        if (query == null) {
            return "Transactions";
        }
        if (query.equals("needsCategorized")) {
            return "Categorize";
        }
        if (query.equals("needsTransferred")) {
            return "Needs transferred";
        }
        if (query.startsWith("category,")) {
            Category category = categoriesById.get(UUID.fromString(query.split(",")[1]));
            return category == null ? "Category" : category.name;
        }
        return "Transactions";
    }

    // ---- One transaction -------------------------------------------------------------------

    @GetMapping("/transactions/{id}")
    public String transaction(
            @PathVariable UUID id,
            @RequestParam(required = false) String query,
            Model model
    ) {
        Transaction transaction = transactionsService.get(id);
        if (transaction.category != null) {
            transaction.category = categoriesService.getCategoriesById().get(transaction.category.id);
        }
        transaction.bankTransaction = bankTransactionsService.get(transaction.bankTransaction.id);

        model.addAttribute("transaction", transaction);
        model.addAttribute("transferAccount", transactionsService.transferAccountFor(id));
        model.addAttribute("query", query);
        return "transaction";
    }

    @PostMapping("/transactions/{id}/amount")
    public String updateAmount(@PathVariable UUID id, @RequestParam BigDecimal amount, @RequestParam(required = false) String query) {
        transactionsService.updateAmount(id, amount);
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/description")
    public String updateDescription(@PathVariable UUID id, @RequestParam String description, @RequestParam(required = false) String query) {
        transactionsService.updateDescription(id, description);
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/description/reset")
    public String resetDescription(@PathVariable UUID id, @RequestParam(required = false) String query) {
        Transaction transaction = transactionsService.get(id);
        BankTransaction bankTransaction = bankTransactionsService.get(transaction.bankTransaction.id);
        transactionsService.updateDescription(id, bankTransaction.description);
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/notes")
    public String updateNotes(@PathVariable UUID id, @RequestParam(required = false) String notes, @RequestParam(required = false) String query) {
        transactionsService.updateNotes(id, notes);
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/date")
    public String updateDate(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String query
    ) {
        transactionsService.updateDate(id, date);
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/type")
    public String updateType(@PathVariable UUID id, @RequestParam String transactionType, @RequestParam(required = false) String query) {
        if ("credit".equals(transactionType) || "debit".equals(transactionType)) {
            transactionsService.updateType(id, transactionType);
        }
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/needs-transferred")
    public String toggleNeedsTransferred(@PathVariable UUID id, @RequestParam(required = false) String query) {
        Transaction transaction = transactionsService.get(id);
        transactionsService.needsTransferred(id, !transaction.needsTransferred);
        return redirectToTransaction(id, query);
    }

    @PostMapping("/transactions/{id}/delete")
    public String delete(@PathVariable UUID id, @RequestParam(required = false) String query) {
        transactionsService.delete(id);
        return "redirect:" + UriComponentsBuilder.fromPath("/transactions").queryParamIfPresent("query", Optional.ofNullable(query)).encode().toUriString();
    }

    // ---- Category ----------------------------------------------------------------------------

    @GetMapping("/transactions/{id}/category")
    public String categoryPicker(@PathVariable UUID id, @RequestParam(required = false) String query, Model model) {
        model.addAttribute("transaction", transactionsService.get(id));
        model.addAttribute("categories", categoriesService.list());
        model.addAttribute("query", query);
        return "transaction-category";
    }

    @PostMapping("/transactions/{id}/category")
    public String updateCategory(@PathVariable UUID id, @RequestParam UUID categoryId, @RequestParam(required = false) String query) {
        transactionsService.updateCategory(id, categoryId);
        return redirectToTransaction(id, query);
    }

    // ---- Tags --------------------------------------------------------------------------------

    @GetMapping("/transactions/{id}/tags")
    public String tagsPicker(@PathVariable UUID id, @RequestParam(required = false) String query, Model model) {
        Transaction transaction = transactionsService.get(id);
        Set<String> selected = new HashSet<>();
        if (transaction.tags != null) {
            for (String tag : transaction.tags.split(",")) {
                selected.add(tag.trim());
            }
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("tags", TAGS);
        model.addAttribute("selected", selected);
        model.addAttribute("query", query);
        return "transaction-tags";
    }

    @PostMapping("/transactions/{id}/tags")
    public String updateTags(
            @PathVariable UUID id,
            @RequestParam(name = "tag", required = false) List<String> tags,
            @RequestParam(required = false) String query
    ) {
        transactionsService.updateTags(id, tags == null ? null : String.join(", ", tags));
        return redirectToTransaction(id, query);
    }

    // ---- Create ------------------------------------------------------------------------------

    @GetMapping("/create-transaction")
    public String createForm(Model model) {
        model.addAttribute("accounts", BankAccounts.ALL);
        model.addAttribute("today", LocalDate.now());
        return "transaction-create";
    }

    @PostMapping("/create-transaction")
    public String create(
            @RequestParam String account,
            @RequestParam String description,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String transactionType
    ) {
        if (!"credit".equals(transactionType) && !"debit".equals(transactionType)) {
            throw new IllegalArgumentException("'transactionType' of 'credit' or 'debit' required");
        }

        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.id = UUID.randomUUID();
        bankTransaction.csv = "UserTransaction" + bankTransaction.id;
        bankTransaction.account = account;
        bankTransaction.description = description;
        bankTransaction.amount = amount.abs();
        bankTransaction.transactionDate = date;
        bankTransaction.postDate = date;
        bankTransaction.transactionType = transactionType;

        bankTransactionsService.add(bankTransaction);
        UUID transactionId = transactionsService.addFrom(bankTransaction);
        return redirectToTransaction(transactionId, null);
    }

    static String redirectToTransaction(UUID id, String query) {
        return "redirect:" + UriComponentsBuilder.fromPath("/transactions/{id}")
                .queryParamIfPresent("query", Optional.ofNullable(query).filter(q -> !q.isBlank()))
                .encode()
                .buildAndExpand(id)
                .toUriString();
    }

}
