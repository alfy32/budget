package com.alfy.budget;

import com.alfy.budget.service.PlansService;
import com.alfy.budget.tools.MockData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The mock data (./gradlew mockDb) builds, and every page works on it. */
@SpringBootTest(properties = "budget.db-path=build/test-db/mock-${random.uuid}.db")
@AutoConfigureMockMvc
class MockDataTest {

    @Autowired
    NamedParameterJdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    PlansService plansService;
    @Autowired
    MockMvc mvc;

    @Test
    void mockDataFillsTwoYearsAndEveryPageWorks() throws Exception {
        LocalDate today = LocalDate.now();
        int count = MockData.create(jdbc, transactionManager, plansService, today);
        assertTrue(count > 1000, "about two years of transactions");

        Map<String, Object> range = jdbc.queryForMap(
                "SELECT min(transaction_date) AS first, max(transaction_date) AS last FROM transactions", Map.of());
        assertEquals(today.withDayOfMonth(1).minusMonths(24).toString(), range.get("first"));
        assertEquals(today.toString(), range.get("last"));
        assertEquals(2, plansService.list().size());
        assertEquals(1, plansService.proposals().size());
        assertEquals(today.withDayOfMonth(1).minusMonths(12), plansService.current().startMonth);

        // It never mixes into a database that has data.
        assertThrows(IllegalStateException.class, () -> MockData.create(jdbc, transactionManager, plansService, today));

        List<String> pages = List.of("/", "/transactions", "/transactions?search=fresh", "/budgets", "/monthly", "/yearly",
                "/needs-transferred", "/plans", "/accounts", "/categories", "/rules", "/create-transaction", "/transactions-upload",
                "/monthly?date=" + today.minusMonths(13), "/yearly?date=" + today.minusYears(1));
        for (String page : pages) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
        for (String table : List.of("plans", "accounts", "budgets", "categories", "transactions", "categorize_rules")) {
            String prefix = switch (table) {
                case "categorize_rules" -> "/rules/";
                default -> "/" + table + "/";
            };
            for (String id : jdbc.queryForList("SELECT id FROM " + table + " LIMIT 3", Map.of(), String.class)) {
                mvc.perform(get(prefix + id)).andExpect(status().isOk());
            }
        }
        String split = jdbc.queryForObject("SELECT bank_transaction_id FROM transactions WHERE split_index > 0 LIMIT 1", Map.of(), String.class);
        mvc.perform(get("/split/" + split)).andExpect(status().isOk());
    }

}
