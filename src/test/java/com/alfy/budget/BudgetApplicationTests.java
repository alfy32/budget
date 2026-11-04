package com.alfy.budget;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;
import com.alfy.budget.model.CategorizeRule.MatchType;
import com.alfy.budget.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
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
    PlansService plansService;
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
        // $700 a month in the current plan
        plansService.setAmounts(plansService.current().id, Map.of(category.id, new BigDecimal("8400")));

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
                "/rules/new",
                "/rules/new?description=GUNNISON%20MARKET&categoryId=" + category.id,
                "/transactions-upload",
                "/accounts",
                "/plans",
                "/plans/" + plansService.current().id,
        };

        for (String page : pages) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }

        mvc.perform(get("/transactions?search=gunnison")).andExpect(content().string(org.hamcrest.Matchers.containsString("GUNNISON MARKET")));

        // Follow the category links on the monthly and yearly pages (they carry dates in the URL).
        for (String report : new String[]{"/monthly", "/yearly"}) {
            String html = mvc.perform(get(report)).andReturn().getResponse().getContentAsString();
            var links = java.util.regex.Pattern.compile("href=\"(/transactions\\?query=category[^\"]*)\"").matcher(html);
            int followed = 0;
            while (links.find()) {
                String link = org.springframework.web.util.HtmlUtils.htmlUnescape(links.group(1));
                assertTrue(link.matches(".*\\d{4}-\\d{2}-\\d{2}.*"), report + " link has ISO dates: " + link);
                mvc.perform(get(java.net.URI.create(link))).andExpect(status().isOk());
                followed++;
            }
            assertTrue(followed > 0, report + " has category links");
        }
        mvc.perform(get("/monthly")).andExpect(content().string(org.hamcrest.Matchers.containsString("$42.50 of $700.00")));

        // Both ways of adding transactions offer the same bank accounts (upload only the ones with a CSV format).
        String manual = mvc.perform(get("/create-transaction")).andReturn().getResponse().getContentAsString();
        String upload = mvc.perform(get("/transactions-upload")).andReturn().getResponse().getContentAsString();
        for (String account : com.alfy.budget.model.BankAccounts.ALL) {
            assertTrue(manual.contains(">" + account + "<"), "add by hand offers " + account);
        }
        for (String account : com.alfy.budget.model.BankAccounts.UPLOADABLE) {
            assertTrue(upload.contains(">" + account + "<"), "upload offers " + account);
            assertTrue(com.alfy.budget.model.BankAccounts.ALL.contains(account), account + " is in the full list");
        }
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
    void transfersFollowTheCategoryUntilPaidBack() throws Exception {
        Budget yearly = budgetsService.add(Budget.create("TransferTest Yearly", new BigDecimal("100")));
        budgetsService.setTransferAccount(yearly.id, "TransferTest A");
        Budget savings = budgetsService.add(Budget.create("TransferTest Savings", new BigDecimal("100")));
        budgetsService.setTransferAccount(savings.id, "TransferTest B");
        Budget plain = budgetsService.add(Budget.create("TransferTest Plain", new BigDecimal("100")));

        Category inYearly = Category.create("TransferTest Car", yearly);
        Category inSavings = Category.create("TransferTest Hired Help", savings);
        Category inPlain = Category.create("TransferTest Snacks", plain);
        categoriesService.add(inYearly);
        categoriesService.add(inSavings);
        categoriesService.add(inPlain);

        BankTransaction bank = new BankTransaction();
        bank.account = "SBSU Checking";
        bank.transactionType = "debit";
        bank.transactionDate = LocalDate.of(2022, 5, 5);
        bank.description = "TRANSFERTEST OIL CHANGE";
        bank.amount = new BigDecimal("80.00");
        bankTransactionsService.add(bank);
        UUID id = transactionsService.addFrom(bank);

        // New and uncategorized: nothing to transfer.
        assertFalse(transactionsService.get(id).needsTransferred);

        // The flag follows the category until it's paid back.
        transactionsService.updateCategory(id, inPlain.id);
        assertFalse(transactionsService.get(id).needsTransferred);
        transactionsService.updateCategory(id, inYearly.id);
        assertTrue(transactionsService.get(id).needsTransferred);
        assertEquals("TransferTest A", transactionsService.transferAccountFor(id));
        transactionsService.updateCategory(id, inPlain.id);
        assertFalse(transactionsService.get(id).needsTransferred);
        transactionsService.updateCategory(id, inYearly.id);

        // Paying it back records where the money came from...
        mvc.perform(post("/needs-transferred/mark").param("account", "TransferTest A"));
        var paid = transactionsService.get(id);
        assertFalse(paid.needsTransferred);
        assertEquals("TransferTest A", paid.transferredFrom);

        // ...and moving it to another transfer budget afterwards doesn't ask for the money again.
        transactionsService.updateCategory(id, inSavings.id);
        assertFalse(transactionsService.get(id).needsTransferred);
        assertEquals("TransferTest A", transactionsService.get(id).transferredFrom);

        // Splitting a paid-back transaction: the new row counts as paid back too.
        mvc.perform(post("/split/" + bank.id)
                .param("rows[0].id", id.toString()).param("rows[0].categoryId", inYearly.id.toString())
                .param("rows[1].id", "").param("rows[1].categoryId", inSavings.id.toString()).param("rows[1].amount", "30.00"));
        for (var row : transactionsService.listWithBankTransactionId(bank.id)) {
            assertFalse(row.needsTransferred);
            assertEquals("TransferTest A", row.transferredFrom);
        }

        // The page shows the switch with the account name, and the home tile only counts real transfers.
        mvc.perform(get("/transactions/" + id))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Needs transfer from TransferTest A")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paid back from TransferTest A")));
        int before = transactionsService.transferSummary().count();
        transactionsService.needsTransferred(id, true);
        assertEquals(before + 1, transactionsService.transferSummary().count());
    }

    @Test
    void plansChangeTheBudgetFromTheirStartMonth() throws Exception {
        Budget bingo = budgetsService.add(Budget.create("PlanTest Bingo", BigDecimal.ZERO));
        budgetsService.setGroup(bingo.id, com.alfy.budget.model.PlanGroup.YEARLY);
        Category chips = Category.create("PlanTest Chips", bingo);
        categoriesService.add(chips);

        // A plan for last year with $1,200/yr, and one from July of last year with $2,400/yr.
        int lastYear = LocalDate.now().getYear() - 1;
        var base = plansService.forMonth(LocalDate.of(lastYear, 1, 1));
        UUID july = plansService.copy(base.id, LocalDate.of(lastYear, 7, 1));
        UUID january = plansService.copy(base.id, LocalDate.of(lastYear, 1, 1));
        try {
            plansService.setAmounts(january, Map.of(chips.id, new BigDecimal("1200")));
            plansService.setAmounts(july, Map.of(chips.id, new BigDecimal("2400")));

            BankTransaction t = new BankTransaction();
            t.account = "SBSU Checking";
            t.transactionType = "debit";
            t.transactionDate = LocalDate.of(lastYear, 8, 10);
            t.description = "PLANTEST BINGO";
            t.amount = new BigDecimal("150.00");
            bankTransactionsService.add(t);
            transactionsService.updateCategory(transactionsService.addFrom(t), chips.id);

            // Month pages use the plan in effect that month: $100 in March, $200 in August.
            var reports = new com.alfy.budget.service.BudgetReportsService(budgetsService, categoriesService, transactionsService, plansService);
            budgetsService.setMonthly(bingo.id, true);
            var march = reports.monthly(LocalDate.of(lastYear, 3, 1)).stream().filter(b -> b.budget.id.equals(bingo.id)).findFirst().orElseThrow();
            var august = reports.monthly(LocalDate.of(lastYear, 8, 1)).stream().filter(b -> b.budget.id.equals(bingo.id)).findFirst().orElseThrow();
            assertEquals(new BigDecimal("100.00"), march.budget.amount);
            assertEquals(new BigDecimal("200.00"), august.budget.amount);
            assertEquals(new BigDecimal("150.00"), august.total);

            // The year adds them up: 6 x $100 + 6 x $200 = $1,800, and a past year is fully "on pace".
            var year = reports.yearly(LocalDate.of(lastYear, 1, 1)).stream().filter(b -> b.budget.id.equals(bingo.id)).findFirst().orElseThrow();
            assertEquals(new BigDecimal("1800.00"), year.budget.amount);
            assertEquals(new BigDecimal("1800.00"), year.expectedTotal);

            // The worksheet shows last plan's amount and the savings math.
            mvc.perform(get("/plans/" + july)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Last $1,200.00")));
            mvc.perform(post("/plans/" + july)
                            .param("startMonth", lastYear + "-07")
                            .param("takeHome", "5000")
                            .param("line_" + chips.id, "3000"))
                    .andExpect(redirectedUrl("/plans/" + july + "?saved=true"));
            var saved = plansService.get(july);
            assertEquals(new BigDecimal("5000.00"), saved.takeHome);
            // Chips went from $1,200/yr ($100/mo) in January's plan to $3,000/yr ($250/mo): +$150 a month of yearly spending.
            assertEquals(new BigDecimal("150.00"),
                    plansService.totals(saved).yearly().subtract(plansService.totals(plansService.get(january)).yearly()));

            // From-income items come off take-home before savings.
            mvc.perform(post("/plans/" + july).param("startMonth", lastYear + "-07").param("takeHome", "5000")
                    .param("action", "addIncome").param("newIncomeName", "Roth IRA").param("newIncomeAmount", "1200"));
            var totals = plansService.totals(plansService.get(july));
            assertEquals(totals.takeHome().subtract(totals.fromIncome()).subtract(totals.spending()), totals.savings());
            assertTrue(totals.fromIncome().compareTo(new BigDecimal("100.00")) >= 0);

            // Two plans can't start the same month.
            mvc.perform(post("/plans/" + july).param("startMonth", lastYear + "-01").param("takeHome", "5000"))
                    .andExpect(status().is3xxRedirection());
            assertEquals(LocalDate.of(lastYear, 7, 1), plansService.get(july).startMonth);

            // A budget that's in a plan can't be deleted (it would vanish from past months).
            mvc.perform(post("/budgets/" + bingo.id + "/delete")).andExpect(status().is3xxRedirection());
            assertEquals("PlanTest Bingo", budgetsService.get(bingo.id).name);
        } finally {
            plansService.delete(july);
            plansService.delete(january);
        }
    }

    @Test
    void aProposedPlanIsIgnoredUntilItsCommitted() throws Exception {
        int committedBefore = plansService.list().size();
        var current = plansService.current();

        String location = mvc.perform(post("/plans")).andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        UUID proposalId = UUID.fromString(location.substring("/plans/".length()));
        try {
            assertTrue(plansService.get(proposalId).isProposed());
            assertEquals(committedBefore, plansService.list().size());
            assertEquals(current.id, plansService.current().id, "a proposal doesn't change the current plan");
            mvc.perform(get(location)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Proposed")));

            // Editing it doesn't touch the current plan.
            mvc.perform(post(location).param("takeHome", "123.45").param("line_" + category.id, "99999"));
            assertEquals(new BigDecimal("8400.00"), plansService.lines(current.id).stream()
                    .filter(line -> line.categoryId.equals(category.id)).findFirst().orElseThrow().amount);

            // Committing it to a month makes it a real plan.
            mvc.perform(post(location).param("takeHome", "123.45").param("action", "start").param("startMonth", "2099-01"))
                    .andExpect(redirectedUrl(location + "?started=true"));
            assertFalse(plansService.get(proposalId).isProposed());
            assertEquals(LocalDate.of(2099, 1, 1), plansService.get(proposalId).startMonth);
            assertEquals(committedBefore + 1, plansService.list().size());
        } finally {
            plansService.delete(proposalId);
        }
    }

    @Test
    void aPlanLineCanBeRenamedAndSplit() throws Exception {
        var current = plansService.current();
        Budget utilities = budgetsService.add(Budget.create("SplitTest Utilities", BigDecimal.ZERO));
        Category power = Category.create("SplitTest Power", utilities);
        categoriesService.add(power);
        plansService.setAmounts(current.id, Map.of(power.id, new BigDecimal("1800")));

        // Split $600 off into a new "Solar" line in the same budget.
        mvc.perform(post("/plans/" + current.id)
                        .param("takeHome", current.takeHome.toPlainString())
                        .param("line_" + power.id, "1800")
                        .param("action", "split:" + power.id)
                        .param("splitName_" + power.id, "SplitTest Solar")
                        .param("splitAmount_" + power.id, "600"))
                .andExpect(redirectedUrl("/plans/" + current.id));
        var lines = plansService.lines(current.id);
        var powerLine = lines.stream().filter(l -> l.categoryId.equals(power.id)).findFirst().orElseThrow();
        var solarLine = lines.stream().filter(l -> l.categoryName.equals("SplitTest Solar")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1200.00"), powerLine.amount);
        assertEquals(new BigDecimal("600.00"), solarLine.amount);
        assertEquals(utilities.id, solarLine.budgetId);

        // Rename from the worksheet; it's the same category everywhere.
        mvc.perform(post("/plans/" + current.id)
                .param("takeHome", current.takeHome.toPlainString())
                .param("rename_" + power.id, "SplitTest Electric"));
        assertEquals("SplitTest Electric", categoriesService.get(power.id).name);
        assertEquals(new BigDecimal("1200.00"), plansService.lines(current.id).stream()
                .filter(l -> l.categoryId.equals(power.id)).findFirst().orElseThrow().amount);
    }

    @Test
    void autoCategorizeUsesRules() {
        autoCategorizeService.add("VANGUARD BUY/INVESTMENT", MatchType.EQUALS, category.id);
        autoCategorizeService.updateCachedData();

        BankTransaction vanguard = new BankTransaction();
        vanguard.description = "VANGUARD BUY/INVESTMENT";
        assertTrue(autoCategorizeService.autoCategorize(vanguard, transactionId));

        BankTransaction other = new BankTransaction();
        other.description = "Something else";
        assertFalse(autoCategorizeService.autoCategorize(other, transactionId));
    }

    @Test
    void ruleCanBePreviewedEditedAndApplied() throws Exception {
        // Three uncategorized "RuleTest Shop" charges with different order codes, plus one already categorized.
        UUID[] ids = new UUID[4];
        for (int i = 0; i < 4; i++) {
            BankTransaction t = new BankTransaction();
            t.account = "SBSU Checking";
            t.transactionType = "debit";
            t.transactionDate = LocalDate.of(2021, 6, 1 + i);
            t.description = "RULETEST SHOP*" + (1000 + i) + "  Online WA";
            t.amount = new BigDecimal("5.00");
            bankTransactionsService.add(t);
            ids[i] = transactionsService.addFrom(t);
        }
        Category other = Category.create("RuleTest Other");
        categoriesService.add(other);
        transactionsService.updateCategory(ids[3], other.id);

        // Exact text doesn't catch the others; "contains" does.
        UUID ruleId = autoCategorizeService.add("RULETEST SHOP*1000  Online WA", MatchType.EQUALS, category.id);
        assertEquals(1, autoCategorizeService.preview(autoCategorizeService.get(ruleId)).matches());

        mvc.perform(post("/rules/" + ruleId)
                        .param("description", "ruletest shop")
                        .param("matchType", "CONTAINS")
                        .param("categoryId", category.id.toString()))
                .andExpect(redirectedUrl("/rules/" + ruleId + "?saved=true"));

        var preview = autoCategorizeService.preview(autoCategorizeService.get(ruleId));
        assertEquals(4, preview.matches());
        assertEquals(3, preview.needsCategory());

        // The live preview and the edit page render.
        mvc.perform(get("/rules/preview").param("description", "RULETEST").param("matchType", "STARTS_WITH"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("4 transactions")));
        mvc.perform(get("/rules/" + ruleId)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Categorize the 3 that need a category")));

        // Apply only fills in the uncategorized ones.
        mvc.perform(post("/rules/" + ruleId + "/apply")).andExpect(redirectedUrl("/rules/" + ruleId + "?applied=3"));
        assertEquals(category.id, transactionsService.get(ids[0]).category.id);
        assertEquals(category.id, transactionsService.get(ids[2]).category.id);
        assertEquals(other.id, transactionsService.get(ids[3]).category.id);

        // A more specific rule wins over the broad one.
        autoCategorizeService.add("RULETEST SHOP*1002", MatchType.STARTS_WITH, other.id);
        assertEquals(3, autoCategorizeService.preview(autoCategorizeService.get(ruleId)).matches());

        mvc.perform(post("/rules/" + ruleId + "/delete")).andExpect(redirectedUrl("/rules"));
    }

}
