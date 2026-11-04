package com.alfy.budget;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;
import com.alfy.budget.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Starts the whole app against a throwaway SQLite file and renders every page,
 * so a typo in a template or a SQL query fails here instead of in the browser.
 */
@SpringBootTest(properties = "budget.db-path=build/test-db/${random.uuid}.db")
@AutoConfigureMockMvc
class BudgetApplicationTests {

    @Autowired
    MockMvc mvc;
    @Autowired
    BudgetsService budgetsService;
    @Autowired
    CategoriesService categoriesService;
    @Autowired
    BankTransactionsService bankTransactionsService;
    @Autowired
    TransactionsService transactionsService;
    @Autowired
    AutoCategorizeService autoCategorizeService;
    @Autowired
    AccountsService accountsService;

    Budget budget;
    Category category;
    BankTransaction bankTransaction;
    UUID transactionId;

    @BeforeEach
    void addData() {
        budget = budgetsService.add(Budget.create("Groceries", new BigDecimal("700")));
        budgetsService.setMonthly(budget.id, true);
        budgetsService.setTransferAccount(budget.id, "Savings");

        category = Category.create("Food", budget);
        categoriesService.add(category);

        bankTransaction = new BankTransaction();
        bankTransaction.account = "SBSU Checking";
        bankTransaction.transactionType = "debit";
        bankTransaction.transactionDate = LocalDate.now();
        bankTransaction.description = "GUNNISON MARKET";
        bankTransaction.amount = new BigDecimal("42.50");
        bankTransactionsService.add(bankTransaction);
        transactionId = transactionsService.addFrom(bankTransaction);
        transactionsService.updateCategory(transactionId, category.id);
    }

    @Test
    void everyPageRenders() throws Exception {
        String today = LocalDate.now().toString();
        String[] pages = {
                "/",
                "/transactions",
                "/transactions?query=needsCategorized",
                "/transactions?query=needsTransferred",
                "/transactions?query=possibleDuplicates",
                "/transactions?query=category," + category.id + "," + today + "," + today,
                "/transactions?query=category," + category.id + ",needsTransferred",
                "/transactions?search=gunnison",
                "/transactions/" + transactionId,
                "/transactions/" + transactionId + "?query=needsCategorized",
                "/transactions/" + transactionId + "/category",
                "/transactions/" + transactionId + "/tags",
                "/split/" + bankTransaction.id,
                "/create-transaction",
                "/monthly",
                "/monthly?date=2024-02-01",
                "/yearly",
                "/needs-transferred",
                "/budgets",
                "/budgets/" + budget.id,
                "/categories",
                "/categories/" + category.id,
                "/rules",
                "/transactions-upload",
                "/accounts",
        };

        for (String page : pages) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }

        mvc.perform(get("/transactions?search=gunnison")).andExpect(content().string(org.hamcrest.Matchers.containsString("GUNNISON MARKET")));
        mvc.perform(get("/monthly")).andExpect(content().string(org.hamcrest.Matchers.containsString("$42.50 of $700.00")));
    }

    @Test
    void editTransaction() throws Exception {
        mvc.perform(post("/transactions/" + transactionId + "/notes").param("notes", "for the party").param("query", "needsCategorized"))
                .andExpect(redirectedUrl("/transactions/" + transactionId + "?query=needsCategorized"));
        mvc.perform(post("/transactions/" + transactionId + "/amount").param("amount", "40.00"));
        mvc.perform(post("/transactions/" + transactionId + "/tags").param("tag", "Gift").param("tag", "Car"));
        mvc.perform(post("/transactions/" + transactionId + "/needs-transferred"));

        var transaction = transactionsService.get(transactionId);
        assertEquals("for the party", transaction.notes);
        assertEquals(new BigDecimal("40.00"), transaction.amount);
        assertEquals("Gift, Car", transaction.tags);
        assertFalse(transaction.needsTransferred);
    }

    @Test
    void splitKeepsTheTotal() throws Exception {
        mvc.perform(post("/split/" + bankTransaction.id)
                        .param("rows[0].id", transactionId.toString())
                        .param("rows[0].categoryId", category.id.toString())
                        .param("rows[0].amount", "999") // ignored: the first row always gets the remainder
                        .param("rows[1].id", "")
                        .param("rows[1].categoryId", "")
                        .param("rows[1].amount", "12.50"))
                .andExpect(status().is3xxRedirection());

        var rows = transactionsService.listWithBankTransactionId(bankTransaction.id);
        assertEquals(2, rows.size());
        assertEquals(new BigDecimal("30.00"), rows.get(0).amount);
        assertEquals(new BigDecimal("12.50"), rows.get(1).amount);
        assertEquals(1, rows.get(0).splitIndex);
        assertEquals("GUNNISON MARKET", rows.get(1).description);
        assertNull(rows.get(1).category);
    }

    @Test
    void deletingACategoryUncategorizesItsTransactions() throws Exception {
        mvc.perform(post("/categories/" + category.id + "/delete")).andExpect(redirectedUrl("/categories"));
        assertNull(transactionsService.get(transactionId).category);
    }

    @Test
    void accountAddsCreditsAndSubtractsDebits() throws Exception {
        UUID accountId = accountsService.add("Gift Money Alan");
        mvc.perform(post("/accounts/" + accountId + "/categories").param("categoryId", category.id.toString()))
                .andExpect(redirectedUrl("/accounts/" + accountId));

        BankTransaction gift = new BankTransaction();
        gift.account = "SBSU Checking";
        gift.transactionType = "credit";
        gift.transactionDate = LocalDate.now().minusDays(1);
        gift.description = "Birthday money";
        gift.amount = new BigDecimal("100.00");
        bankTransactionsService.add(gift);
        transactionsService.updateCategory(transactionsService.addFrom(gift), category.id);

        // +100.00 the day before, then -42.50 from addData()
        assertEquals(new BigDecimal("57.50"), accountsService.get(accountId).balance);

        var history = accountsService.history(accountId);
        assertEquals(2, history.size());
        assertEquals(new BigDecimal("57.50"), history.get(0).balanceAfter()); // newest first
        assertEquals(new BigDecimal("100.00"), history.get(1).balanceAfter());

        mvc.perform(get("/accounts")).andExpect(content().string(org.hamcrest.Matchers.containsString("$57.50")));
        mvc.perform(get("/accounts/" + accountId)).andExpect(status().isOk());
        mvc.perform(get("/categories/" + category.id)).andExpect(status().isOk());

        // Its category has a budget, so it still shows on the yearly page.
        categoriesService.setName(category.id, "Account Only Category");
        mvc.perform(get("/yearly")).andExpect(content().string(org.hamcrest.Matchers.containsString(">Account Only Category<")));

        // With no budget, an account's category is left off the yearly page (not even as Not Categorized).
        String notCategorizedBefore = notCategorizedOnYearly();
        mvc.perform(post("/categories/" + category.id + "/budget"));
        mvc.perform(get("/yearly"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Account Only Category"))));
        assertEquals(notCategorizedBefore, notCategorizedOnYearly());

        // Spending more than is in it goes negative.
        transactionsService.updateAmount(transactionId, new BigDecimal("150.00"));
        assertEquals(new BigDecimal("-50.00"), accountsService.get(accountId).balance);
    }

    private String notCategorizedOnYearly() throws Exception {
        String html = mvc.perform(get("/yearly")).andReturn().getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern.compile("Not Categorized</div>\\s*<div class=\"amount small\">[^<]*").matcher(html);
        assertTrue(matcher.find(), "yearly page shows a Not Categorized row");
        return matcher.group();
    }

    @Test
    void longListsLoadFiftyAtATime() throws Exception {
        LocalDate day = LocalDate.of(2020, 3, 14);
        for (int i = 0; i < 60; i++) {
            BankTransaction t = new BankTransaction();
            t.account = "SBSU Checking";
            t.transactionType = "debit";
            t.transactionDate = day;
            t.description = String.format("PagingTest %02d", i);
            t.amount = new BigDecimal("1.00");
            bankTransactionsService.add(t);
            transactionsService.addFrom(t);
        }

        String first = mvc.perform(get("/transactions").param("search", "pagingtest"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(50, count(first, "PagingTest "));
        assertTrue(first.contains("Sat, Mar 14, 2020"), "first batch has the day heading");
        assertTrue(first.contains("hx-trigger=\"revealed\""), "first batch ends with a load-more placeholder");
        assertTrue(first.contains("page=1"));

        String second = mvc.perform(get("/transactions")
                        .param("search", "pagingtest").param("page", "1").param("prevDate", day.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(10, count(second, "PagingTest "));
        assertFalse(second.contains("Sat, Mar 14, 2020"), "same day continues without a second heading");
        assertFalse(second.contains("revealed"), "last batch has no placeholder");
        assertFalse(second.contains("<html"), "later batches are just the rows");
        assertTrue(second.contains("PagingTest 59") || first.contains("PagingTest 59"));

        // Search is done in SQL: % is matched literally, not as a wildcard.
        String none = mvc.perform(get("/transactions").param("search", "Paging%Test"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, count(none, "PagingTest "));
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            count++;
        }
        return count;
    }

    @Test
    void autoCategorizeUsesRules() {
        autoCategorizeService.add("VANGUARD BUY/INVESTMENT", category.id);
        autoCategorizeService.updateCachedData();

        BankTransaction vanguard = new BankTransaction();
        vanguard.description = "VANGUARD BUY/INVESTMENT";
        assertTrue(autoCategorizeService.autoCategorize(vanguard, transactionId));

        BankTransaction other = new BankTransaction();
        other.description = "Something else";
        assertFalse(autoCategorizeService.autoCategorize(other, transactionId));
    }

}
