package com.alfy.budget.tools;

import com.alfy.budget.BudgetApplication;
import com.alfy.budget.service.PlansService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;

/**
 * Makes a database full of made-up data for working on the app: budgets, categories, two committed plans and a
 * proposal, an account, rules, and about two years of transactions ending today (paychecks, bills, groceries,
 * yearly spending waiting to be transferred, a few split and uncategorized ones).
 *
 *   ./gradlew mockDb                       creates data/mock.db
 *   ./gradlew bootRun -Pmock               runs the app on it
 *
 * Everything is fake (the repo is public). Dates count back from today, and the random numbers use a fixed
 * seed, so every run gives the same data, just moved to the current 24 months.
 */
public final class MockData {

    // Bank accounts, from BankAccounts.ALL.
    private static final String CHECKING = "SBSU Checking";
    private static final String CARD = "Zions Agility Cash Visa";
    private static final String CASH = "Cash";

    private final NamedParameterJdbcTemplate jdbc;
    private final LocalDate today;
    private final LocalDate start;
    private final LocalDate raise;
    private final Random random = new Random(24);

    private final Map<String, String> budgetIds = new HashMap<>();
    private final Map<String, String> budgetTransfer = new HashMap<>();
    private final Map<String, String> categoryIds = new HashMap<>();
    private final Map<String, String> categoryBudget = new HashMap<>();

    // Running totals that later transactions pay back.
    private final Map<YearMonth, Long> cardSpending = new HashMap<>();
    private final Map<String, Map<YearMonth, Long>> transferSpending = new HashMap<>();
    private int transactions;
    private int reference = 100000;

    private MockData(NamedParameterJdbcTemplate jdbc, LocalDate today) {
        this.jdbc = jdbc;
        this.today = today;
        this.start = today.withDayOfMonth(1).minusMonths(24);
        this.raise = today.withDayOfMonth(1).minusMonths(12);
    }

    public static void main(String[] args) {
        // devtools would restart this main() with Spring's arguments instead of ours.
        System.setProperty("spring.devtools.restart.enabled", "false");
        Path path = Path.of(args.length > 0 ? args[0] : "data/mock.db");
        if (Files.exists(path)) {
            System.err.println(path + " already exists. Delete it (and any " + path.getFileName() + "-wal / -shm) to make a new one.");
            System.exit(1);
        }
        // Command-line arguments beat application.properties (default properties don't: they'd quietly fall back
        // to DB_PATH or data/budget.db).
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BudgetApplication.class)
                .web(WebApplicationType.NONE)
                .run("--budget.db-path=" + path, "--spring.main.banner-mode=off", "--logging.level.root=warn")) {
            String opened = context.getEnvironment().getProperty("budget.db-path");
            if (opened == null || !Path.of(opened).toAbsolutePath().equals(path.toAbsolutePath())) {
                throw new IllegalStateException("Opened " + opened + " instead of " + path + "; not touching it.");
            }
            int count = create(context.getBean(NamedParameterJdbcTemplate.class),
                    context.getBean(PlatformTransactionManager.class), context.getBean(PlansService.class), LocalDate.now());
            System.out.println("Made " + path + " with " + count + " transactions. Run the app on it with:");
            System.out.println(path.equals(Path.of("data/mock.db")) ? "  ./gradlew bootRun -Pmock" : "  DB_PATH=" + path + " ./gradlew bootRun");
        }
    }

    /**
     * Fills an empty database (tables already made by Flyway). Returns how many transactions it made.
     * Refuses a database that has any transactions, budgets or categories, so it can never mix into real data.
     */
    public static int create(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                             PlansService plansService, LocalDate today) {
        Integer existing = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM transactions) + (SELECT count(*) FROM budgets) + (SELECT count(*) FROM categories)
                """, Map.of(), Integer.class);
        if (existing == null || existing > 0) {
            throw new IllegalStateException("This database already has data; mock data only goes into a new, empty one.");
        }
        MockData mock = new MockData(jdbc, today);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> mock.fill());
        plansService.syncCurrentPlan();
        return mock.transactions;
    }

    private void fill() {
        budgetsAndPlans();
        accountsAndRules();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            day(day);
        }
    }

    // ---- Budgets, categories and plans ----------------------------------------------------------

    /** A category in the plans: its budget and its amount per year in each plan (0 = not in that plan). */
    private record Line(String category, String budget, int first, int second, int proposed, String unit) {
    }

    private void budgetsAndPlans() {
        // name, group, transfer account, shown on the Month page
        budget("Groceries", "monthly", null, true);
        budget("Eating Out", "monthly", null, true);
        budget("Gas & Fuel", "monthly", null, true);
        budget("Kids", "monthly", null, true);
        budget("Fun Money", "monthly", null, true);
        budget("Mortgage", "monthly", null, false);
        budget("Bills & Utilities", "monthly", null, false);
        budget("Car", "yearly", "Yearly", false);
        budget("Insurance", "yearly", "Yearly", false);
        budget("Gifts", "yearly", "Yearly", false);
        budget("Vacation", "yearly", "Yearly", false);
        budget("Clothing", "yearly", "Yearly", false);
        budget("Home Upkeep", "yearly", "Yearly", false);
        budget("Donations", "income", null, false);
        budget("Medical", "other", "HSA", false);
        budget("Old Hobby", "monthly", null, false); // not in any plan

        List<Line> lines = List.of(
                new Line("Groceries", "Groceries", 8400, 9000, 9600, "year"),
                new Line("Household Supplies", "Groceries", 900, 960, 960, "year"),
                new Line("Restaurants", "Eating Out", 1800, 1800, 1500, "year"),
                new Line("Fast Food", "Eating Out", 900, 1080, 900, "year"),
                new Line("Gas", "Gas & Fuel", 2400, 2600, 2800, "year"),
                new Line("School Lunch", "Kids", 480, 540, 540, "month"),
                new Line("Kids Activities", "Kids", 1200, 800, 800, "year"),
                new Line("Music Lessons", "Kids", 0, 420, 480, "month"), // split off Kids Activities in the second plan
                new Line("Fun Money", "Fun Money", 1200, 1200, 1200, "month"),
                new Line("Mortgage", "Mortgage", 17400, 17400, 17400, "month"),
                new Line("Electric", "Bills & Utilities", 1200, 1320, 1320, "year"),
                new Line("Natural Gas", "Bills & Utilities", 900, 900, 960, "year"),
                new Line("Water & Sewer", "Bills & Utilities", 720, 780, 780, "year"),
                new Line("Internet", "Bills & Utilities", 780, 780, 780, "month"),
                new Line("Phone", "Bills & Utilities", 660, 720, 720, "month"),
                new Line("Car Repair", "Car", 800, 1000, 1000, "year"),
                new Line("Oil Change", "Car", 300, 300, 300, "year"),
                new Line("Car Registration", "Car", 150, 160, 160, "year"),
                new Line("Car Insurance", "Insurance", 1500, 1650, 1700, "year"),
                new Line("Home Insurance", "Insurance", 1100, 1200, 1250, "year"),
                new Line("Life Insurance", "Insurance", 480, 480, 480, "month"),
                new Line("Christmas", "Gifts", 1200, 1200, 1200, "year"),
                new Line("Birthdays", "Gifts", 500, 500, 600, "year"),
                new Line("Vacation", "Vacation", 2500, 3000, 3500, "year"),
                new Line("Clothes", "Clothing", 1200, 1200, 1200, "year"),
                new Line("Home Repair", "Home Upkeep", 1000, 1200, 1200, "year"),
                new Line("Property Tax", "Home Upkeep", 2200, 2300, 2400, "year"),
                new Line("Charity", "Donations", 6240, 6600, 6600, "year"),
                new Line("Medical", "Medical", 1500, 1500, 1500, "year"));

        for (Line line : lines) {
            category(line.category(), line.budget());
        }
        // Not in a plan: money moving around, not spending.
        for (String name : List.of("Paycheck", "Interest", "Transfer", "Credit Card Payment", "Cash", "Reimbursement", "Hobby Supplies")) {
            category(name, null);
        }

        // Flyway starts every database with a placeholder plan; replace it.
        jdbc.update("DELETE FROM plans", Map.of());
        String first = plan(start, "Our first plan in the app");
        String second = plan(raise, "Raise; music lessons split off kids activities");
        String proposed = plan(null, "Groceries keep going up");
        for (Line line : lines) {
            planLine(first, line, line.first());
            planLine(second, line, line.second());
            planLine(proposed, line, line.proposed());
        }
    }

    private void budget(String name, String group, String transferAccount, boolean monthly) {
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                        INSERT INTO budgets (id, name, amount, monthly, transfer_account, plan_group)
                        VALUES (:id, :name, 0, :monthly, :transfer, :group)
                        """,
                new MapSqlParameterSource().addValue("id", id).addValue("name", name).addValue("monthly", monthly ? 1 : 0)
                        .addValue("transfer", transferAccount).addValue("group", group));
        budgetIds.put(name, id);
        if (transferAccount != null) {
            budgetTransfer.put(name, transferAccount);
        }
    }

    private void category(String name, String budget) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO categories (id, name) VALUES (:id, :name)", Map.of("id", id, "name", name));
        categoryIds.put(name, id);
        if (budget != null) {
            categoryBudget.put(name, budget);
        }
    }

    private String plan(LocalDate startMonth, String notes) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO plans (id, start_month, notes) VALUES (:id, :start, :notes)",
                new MapSqlParameterSource().addValue("id", id)
                        .addValue("start", startMonth == null ? null : startMonth.toString()).addValue("notes", notes));
        return id;
    }

    private void planLine(String planId, Line line, int dollarsPerYear) {
        if (dollarsPerYear == 0) {
            return;
        }
        jdbc.update("""
                        INSERT INTO plan_lines (plan_id, category_id, budget_id, amount, unit)
                        VALUES (:plan, :category, :budget, :amount, :unit)
                        """,
                new MapSqlParameterSource().addValue("plan", planId).addValue("category", categoryIds.get(line.category()))
                        .addValue("budget", budgetIds.get(line.budget())).addValue("amount", dollarsPerYear * 100)
                        .addValue("unit", line.unit()));
    }

    private void accountsAndRules() {
        category("Birthday Money", null);
        String account = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO accounts (id, name) VALUES (:id, 'Birthday Money')", Map.of("id", account));
        jdbc.update("UPDATE categories SET account_id = :account WHERE id = :id",
                Map.of("account", account, "id", categoryIds.get("Birthday Money")));

        rule("WIDGET WORKS PAYROLL", "equals", "Paycheck");
        rule("FIRST HOME LENDING MTG PMT", "equals", "Mortgage");
        rule("FRESH MARKET", "starts_with", "Groceries");
        rule("QUICKSTOP", "contains", "Gas");
        rule("CITY POWER", "contains", "Electric");
    }

    private void rule(String description, String matchType, String category) {
        jdbc.update("INSERT INTO categorize_rules (id, description, category_id, match_type) VALUES (:id, :description, :category, :match)",
                Map.of("id", UUID.randomUUID().toString(), "description", description, "category", categoryIds.get(category), "match", matchType));
    }

    // ---- A day of transactions -----------------------------------------------------------------

    private void day(LocalDate day) {
        int dom = day.getDayOfMonth();
        Month month = day.getMonth();
        boolean afterRaise = !day.isBefore(raise);
        YearMonth thisMonth = YearMonth.from(day);

        // Pay and giving, twice a month.
        if (dom == 1 || dom == 15) {
            long pay = (afterRaise ? 275000 : 260000) + (dom == 15 && month == Month.DECEMBER ? 50000 : 0);
            credit(CHECKING, day, "WIDGET WORKS PAYROLL", pay, "Paycheck");
            debit(CHECKING, day, "GIVE CHARITY DONATION", pay / 10, "Charity");
        }

        // Bills.
        if (dom == 1) debit(CHECKING, day, "FIRST HOME LENDING MTG PMT", 145000, "Mortgage");
        if (dom == 3) debit(CHECKING, day, "STEADY LIFE INS PREMIUM", 4000, "Life Insurance");
        if (dom == 5) debit(CARD, day, "VALLEYNET INTERNET", 6500, "Internet");
        if (dom == 8) debit(CARD, day, "TALKMORE WIRELESS", afterRaise ? 6000 : 5500, "Phone");
        if (dom == 10) debit(CHECKING, day, "CITY POWER & LIGHT BILL", 6500 + (long) (summer(month) * 9000) + cents(0, 15), "Electric");
        if (dom == 12) debit(CHECKING, day, "MOUNTAIN GAS CO", 2500 + (long) (winter(month) * 11000) + cents(0, 10), "Natural Gas");
        if (dom == 20) debit(CHECKING, day, "CITY UTILITIES WATER", cents(48, 75), "Water & Sewer");

        // Paying the card for last month, and paying checking back for last month's yearly and medical spending.
        if (dom == 2) {
            long card = cardSpending.getOrDefault(thisMonth.minusMonths(1), 0L);
            if (card > 0) {
                debit(CHECKING, day, "ZIONS VISA ONLINE PAYMENT", card, "Credit Card Payment");
                credit(CARD, day, "PAYMENT THANK YOU", card, "Credit Card Payment");
            }
        }
        if (dom == 4) {
            for (String account : List.of("Yearly", "HSA")) {
                long owed = transferSpending.getOrDefault(account, Map.of()).getOrDefault(thisMonth.minusMonths(1), 0L);
                if (owed > 0) {
                    credit(CHECKING, day, "TRANSFER FROM " + account.toUpperCase(), owed, "Transfer");
                }
            }
        }
        if (dom == 25) debit(CHECKING, day, "TRANSFER TO SAVINGS", 40000, "Transfer");
        if (day.equals(thisMonth.atEndOfMonth())) credit(CHECKING, day, "INTEREST PAID", cents(1, 4), "Interest");

        // Everyday spending.
        if (day.getDayOfWeek() == DayOfWeek.SATURDAY) debit(CARD, day, "FRESH MARKET #214", cents(90, 230), "Groceries");
        if (day.getDayOfWeek() == DayOfWeek.WEDNESDAY && chance(0.4)) debit(CARD, day, "FRESH MARKET #214", cents(15, 60), "Groceries");
        if (dom == 7 || dom == 21) debit(CARD, day, "BIG BOX STORE", cents(15, 60), "Household Supplies");
        if (dom == 17) warehouseClub(day);
        if (chance(0.15)) debit(CARD, day, pick("THE GREEN FORK", "PIZZA PALACE", "NOODLE HOUSE", "TACO TIME GRILL"), cents(25, 90), "Restaurants");
        if (chance(0.12)) debit(CARD, day, pick("BURGER BARN", "CHICKEN SHACK", "SUB STOP"), cents(8, 30), "Fast Food");
        if (chance(0.13)) debit(CARD, day, "QUICKSTOP FUEL #4", cents(35, 65), "Gas");
        if (dom == 4 && month != Month.JUNE && month != Month.JULY) debit(CARD, day, "SCHOOL LUNCH ONLINE", cents(40, 55), "School Lunch");
        if (chance(0.03)) debit(CARD, day, pick("REC CENTER", "SOCCER CLUB"), cents(20, 120), "Kids Activities");
        if (afterRaise && dom == 6) debit(CARD, day, "HARMONY MUSIC LESSONS", 3500, "Music Lessons");
        if (chance(0.06)) debit(CARD, day, pick("GAME HAVEN", "BOOK NOOK", "MOVIE PLEX"), cents(10, 80), "Fun Money");
        if (dom == 9 && month.getValue() % 2 == 0) debit(CHECKING, day, "ATM WITHDRAWAL", 6000, "Cash");
        if (dom == 9 && month.getValue() % 2 == 0) debit(CASH, day, "Farmers market", cents(10, 30), "Groceries");

        // Yearly spending (paid back from the Yearly account).
        if (dom == 14 && month.getValue() % 4 == 0) debit(CARD, day, "LUBE QUICK", cents(60, 85), "Oil Change");
        if (chance(0.006)) note(debit(CARD, day, "MAIN ST AUTO REPAIR", cents(150, 700), "Car Repair"), pick("Brakes", "New tires", "Battery"));
        if (month == Month.MARCH && dom == 18) debit(CARD, day, "DMV ONLINE REGISTRATION", cents(140, 160), "Car Registration");
        if ((month == Month.JANUARY || month == Month.JULY) && dom == 9) debit(CHECKING, day, "ROADSAFE INSURANCE", afterRaise ? 82500 : 75000, "Car Insurance");
        if (month == Month.SEPTEMBER && dom == 9) debit(CHECKING, day, "HOMESHIELD INSURANCE", afterRaise ? 120000 : 110000, "Home Insurance");
        if ((month == Month.NOVEMBER && dom >= 20 || month == Month.DECEMBER && dom <= 22) && chance(0.25)) {
            debit(CARD, day, pick("TOY TOWN", "BIG BOX STORE", "ONLINE MARKETPLACE"), cents(30, 200), "Christmas");
        }
        if (isBirthday(day)) {
            debit(CARD, day, "GIFT GALLERY", cents(40, 120), "Birthdays");
            credit(CHECKING, day, "MOBILE DEPOSIT", 5000, "Birthday Money"); // grandma's check for the birthday kid
        }
        if (chance(0.01)) debit(CARD, day, "TOY TOWN", cents(15, 40), "Birthday Money");
        if (month == Month.JULY && dom >= 10 && dom <= 14) {
            tag(debit(CARD, day, pick("LAKESIDE LODGE", "CANYON GRILL", "PARK ENTRY FEE"), cents(80, 450), "Vacation"), "vacation-" + day.getYear());
        }
        if (chance(0.03)) debit(CARD, day, "STYLE OUTLET", cents(30, 150), "Clothes");
        if (chance(0.01)) debit(CARD, day, "HARDWARE DEPOT", cents(25, 300), "Home Repair");
        if (month == Month.NOVEMBER && dom == 28) debit(CHECKING, day, "COUNTY TREASURER", afterRaise ? 230000 : 220000, "Property Tax");

        // Medical (paid back from the HSA), work reimbursements, and a category no plan has anymore.
        if (chance(0.01)) debit(CARD, day, pick("VALLEY CLINIC", "CORNER PHARMACY"), cents(25, 250), "Medical");
        if ((month == Month.APRIL || month == Month.OCTOBER) && dom == 16) credit(CHECKING, day, "WIDGET WORKS EXPENSE REIMB", cents(40, 200), "Reimbursement");
        if (day.isBefore(start.plusMonths(6)) && chance(0.02)) debit(CARD, day, "CRAFT CORNER", cents(10, 60), "Hobby Supplies");
    }

    /** One warehouse trip split three ways: groceries, household supplies and clothes. */
    private void warehouseClub(LocalDate day) {
        long total = cents(180, 320);
        String bank = bankTransaction(CARD, "debit", day, "WAREHOUSE CLUB #88", total);
        long groceries = total * 60 / 100;
        long household = total * 25 / 100;
        transaction(bank, 1, CARD, "debit", day, "WAREHOUSE CLUB #88", groceries, "Groceries");
        transaction(bank, 2, CARD, "debit", day, "WAREHOUSE CLUB #88", household, "Household Supplies");
        transaction(bank, 3, CARD, "debit", day, "WAREHOUSE CLUB #88", total - groceries - household, "Clothes");
    }

    // ---- Writing transactions ------------------------------------------------------------------

    private String debit(String account, LocalDate day, String description, long amount, String category) {
        return transaction(bankTransaction(account, "debit", day, description, amount), -1, account, "debit", day, description, amount, category);
    }

    private String credit(String account, LocalDate day, String description, long amount, String category) {
        return transaction(bankTransaction(account, "credit", day, description, amount), -1, account, "credit", day, description, amount, category);
    }

    private String bankTransaction(String account, String type, LocalDate day, String description, long amount) {
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                        INSERT INTO bank_transactions (id, account, transaction_type, transaction_date, post_date, reference_number, description, amount)
                        VALUES (:id, :account, :type, :date, :post, :reference, :description, :amount)
                        """,
                new MapSqlParameterSource().addValue("id", id).addValue("account", account).addValue("type", type)
                        .addValue("date", day.toString()).addValue("post", day.plusDays(1).toString())
                        .addValue("reference", account.equals(CASH) ? null : String.valueOf(reference++))
                        .addValue("description", description).addValue("amount", amount));
        return id;
    }

    /**
     * Spending in a budget with a transfer account is paid back the next month: older spending records where the
     * money came from, the last three weeks still need transferring. About half of last week's card spending isn't
     * categorized yet.
     */
    private String transaction(String bankId, int splitIndex, String account, String type, LocalDate day, String description,
                               long amount, String category) {
        boolean recent = day.isAfter(today.minusDays(7));
        boolean uncategorized = recent && splitIndex == -1 && account.equals(CARD) && type.equals("debit") && chance(0.5);
        String transferAccount = uncategorized ? null : budgetTransfer.get(categoryBudget.get(category));
        boolean needsTransferred = transferAccount != null && day.isAfter(today.minusDays(21));

        if (account.equals(CARD) && type.equals("debit")) {
            cardSpending.merge(YearMonth.from(day), amount, Long::sum);
        }
        if (transferAccount != null && type.equals("debit")) {
            transferSpending.computeIfAbsent(transferAccount, key -> new HashMap<>()).merge(YearMonth.from(day), amount, Long::sum);
        }

        String id = UUID.randomUUID().toString();
        jdbc.update("""
                        INSERT INTO transactions (id, bank_transaction_id, split_index, account, transaction_type, transaction_date,
                                                  description, amount, category_id, needs_transferred, transferred_from)
                        VALUES (:id, :bank, :split, :account, :type, :date, :description, :amount, :category, :needs, :from)
                        """,
                new MapSqlParameterSource().addValue("id", id).addValue("bank", bankId).addValue("split", splitIndex)
                        .addValue("account", account).addValue("type", type).addValue("date", day.toString())
                        .addValue("description", description).addValue("amount", amount)
                        .addValue("category", uncategorized ? null : categoryIds.get(category))
                        .addValue("needs", needsTransferred ? 1 : 0)
                        .addValue("from", transferAccount != null && !needsTransferred ? transferAccount : null));
        transactions++;
        return id;
    }

    private void note(String transactionId, String notes) {
        jdbc.update("UPDATE transactions SET notes = :notes WHERE id = :id", Map.of("notes", notes, "id", transactionId));
    }

    private void tag(String transactionId, String tags) {
        jdbc.update("UPDATE transactions SET tags = :tags WHERE id = :id", Map.of("tags", tags, "id", transactionId));
    }

    // ---- Randomness ----------------------------------------------------------------------------

    private boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    /** A random amount in cents between two dollar amounts. */
    private long cents(int fromDollars, int toDollars) {
        return fromDollars * 100L + random.nextInt((toDollars - fromDollars) * 100 + 1);
    }

    private String pick(String... choices) {
        return choices[random.nextInt(choices.length)];
    }

    /** 0 in winter, 1 in July. */
    private static double summer(Month month) {
        return (1 - Math.cos(2 * Math.PI * (month.getValue() - 1) / 12.0)) / 2;
    }

    private static double winter(Month month) {
        return 1 - summer(month);
    }

    private static boolean isBirthday(LocalDate day) {
        return day.getMonth() == Month.FEBRUARY && day.getDayOfMonth() == 11
                || day.getMonth() == Month.MAY && day.getDayOfMonth() == 3
                || day.getMonth() == Month.AUGUST && day.getDayOfMonth() == 19
                || day.getMonth() == Month.OCTOBER && day.getDayOfMonth() == 27;
    }

}
