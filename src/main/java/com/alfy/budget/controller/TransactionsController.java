package com.alfy.budget.controller;

import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/transactions")
public class TransactionsController {

    final Logger logger = LoggerFactory.getLogger(TransactionsUploadController.class);

    private static final DateTimeFormatter TRANSACTIONS_PAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd");

    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public void getTransactions(
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Transaction List</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/transactions.css?cache-buster=" + UUID.randomUUID() + "\">");
            printWriter.print("</head>");
            printWriter.print("<body>");

            printWriter.print("<a href=\"/\">Back to Main Page</a>");

            printWriter.print("<div class=\"list\">");
            printWriter.print("<hr>");
            List<Transaction> transactions = transactionsService.getTransactionsOrderedByDate();
            for (Transaction transaction : transactions) {
                String category = transaction.category == null ? "Needs Categorized" : transaction.category;

                printWriter.print("<a class=\"transaction_link\" href=\"/transactions/" + transaction.id + "\">");
                printWriter.print("  <div class=\"transaction_block\">");
                printWriter.print("    <div class=\"transaction_description\">" + transaction.description + "</div>");
                printWriter.print("    <div class=\"transaction_category\">" + category + "</div>");
                printWriter.print("    <div class=\"transaction_date\">" + TRANSACTIONS_PAGE_DATE_FORMATTER.format(transaction.date) + "</div>");
                printWriter.print("    <div class=\"transaction_amount\">" + transaction.getFormattedAmount() + "</div>");
                printWriter.print("  </div>");
                printWriter.print("</a>");
                printWriter.print("<hr>");
            }
            printWriter.print("</div>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

}
