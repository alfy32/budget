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
