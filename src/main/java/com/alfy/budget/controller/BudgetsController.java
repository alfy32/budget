package com.alfy.budget.controller;

import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/budgets")
public class BudgetsController {

    private final TransactionsService transactionsService;

    public BudgetsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public void show(
            @RequestParam(name = "date", required = false) LocalDate date,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Transaction By Category</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("</head>");
            printWriter.print("<body>");

            printWriter.print("<a href=\"/\">Back to Main Page</a>");

            if (date == null) {
                date = LocalDate.now();
            }
            LocalDate start = date.withDayOfMonth(1);
            LocalDate end = date.withDayOfMonth(date.getMonth().length(date.isLeapYear()));
            List<Transaction> transactions = transactionsService.getTransactions(start, end);

            Map<String, Integer> totalsPerCategory = new HashMap<>();
            for (Transaction transaction : transactions) {
                String transactionCategory = transaction.category == null || transaction.category.isEmpty() ? "Not Categorized" : transaction.category;
                Integer value = totalsPerCategory.getOrDefault(transactionCategory, 0);
                totalsPerCategory.put(transactionCategory, value + transaction.amount);
            }

            totalsPerCategory.forEach((String category, Integer amount) -> {
                printWriter.print("<p>" + category + ": " + NumberFormat.getCurrencyInstance().format(amount / 100d) + "</p>");
            });

            printWriter.print("<a href=\"/budgets?date="+ start.minusMonths(1) +"\">Previous</a> ");
            printWriter.print("<a href=\"/budgets?date="+ start.plusMonths(1) +"\">Next</a>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

}
