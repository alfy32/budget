package com.alfy.budget.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping(path = "/transactions")
public class TransactionsController {

    final Logger logger = LoggerFactory.getLogger(TransactionsUploadController.class);

    private static final DateTimeFormatter TRANSACTIONS_PAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TransactionsController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping
    public void getTransactions(
            HttpServletResponse response
    ) throws IOException {

        long start = System.currentTimeMillis();

        String cacheBusterString = UUID.randomUUID().toString();

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Transaction List</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/transactions.css?cache-buster=" + cacheBusterString + "\">");
            printWriter.print("</head>");
            printWriter.print("<body>");

            printWriter.print("<a href=\"/\">Back to Main Page</a>");

            String query = "SELECT *" +
                    " FROM transactions" +
                    " ORDER By transaction_date DESC";

            printWriter.print("<div class=\"list\">");
            printWriter.print("<hr>");
            namedParameterJdbcTemplate.query(query, (ResultSet resultSet) -> {
                printWriter.print("<a class=\"transaction_link\" href=\"/transactions/" + resultSet.getString("id") + "\">");
                printWriter.print("  <div class=\"transaction_block\">");

                printWriter.print("    <div class=\"transaction_description\">");
                printWriter.print(resultSet.getString("description"));
                printWriter.print("    </div>");

                String category = resultSet.getString("category");
                if (category == null) {
                    category = "Needs Categorized";
                }
                printWriter.print("    <div class=\"transaction_category\">");
                printWriter.print(category);
                printWriter.print("    </div>");

                printWriter.print("    <div class=\"transaction_date\">");
                printWriter.print(TRANSACTIONS_PAGE_DATE_FORMATTER.format(LocalDate.parse(resultSet.getString("transaction_date"))));
                printWriter.print("    </div>");

                printWriter.print("    <div class=\"transaction_amount\">");
                printWriter.print(NumberFormat.getCurrencyInstance().format(resultSet.getInt("amount") / 100d));
                printWriter.print("    </div>");

                printWriter.print("  </div>");
                printWriter.print("</a>");
                printWriter.print("<hr>");
            });
            printWriter.print("</div>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }

        long end = System.currentTimeMillis();
        logger.info("Request timing: " + (end - start));
    }

}
