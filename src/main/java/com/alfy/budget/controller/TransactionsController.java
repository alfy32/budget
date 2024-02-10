package com.alfy.budget.controller;

import jakarta.servlet.http.HttpServletResponse;
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
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping(path = "/transactions")
public class TransactionsController {

    private static final DateTimeFormatter TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
    private static final DateTimeFormatter TRANSACTIONS_PAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TransactionsController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping
    public void getTransactions(
            HttpServletResponse response
    ) throws IOException {

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
    }

    @GetMapping(path = "/{transactionId}")
    public void getTransaction(
            @PathVariable("transactionId") int transactionId,
            HttpServletResponse response
    ) throws IOException {

        String cacheBusterString = UUID.randomUUID().toString();

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Transaction</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/transaction.css?cache-buster=" + cacheBusterString + "\">");
            printWriter.print("  <script src=\"/transaction.js?cache-buster=" + cacheBusterString + "\"></script>");
            printWriter.print("</head>");
            printWriter.print("<body>");

            printWriter.print("<a href=\"/transactions\">Back</a>");
            printWriter.print("<div class=\"list\">");
            String query = "SELECT * FROM transactions" +
                    " WHERE id=:id";
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("id", transactionId);
            namedParameterJdbcTemplate.query(query, paramMap, (ResultSet resultSet) -> {
                printWriter.print("<div class=\"transaction_amount\">");
                printWriter.print(NumberFormat.getCurrencyInstance().format(resultSet.getInt("amount") / 100d));
                printWriter.print("</div>");
                printWriter.print("<div class=\"transaction_description\">");
                printWriter.print(resultSet.getString("description"));
                printWriter.print("</div>");
                printWriter.print("<div class=\"transaction_date\">");
                printWriter.print(TRANSACTION_DATE_FORMATTER.format(LocalDate.parse(resultSet.getString("transaction_date"))));
                printWriter.print("</div>");
                printWriter.print("<div class=\"transaction_account\">");
                printWriter.print(resultSet.getString("account"));
                printWriter.print("</div>");
                printWriter.print("<br>");
                printWriter.print("<hr>");
                printWriter.print("<div class=\"selection_title\">Merchant</div>");
                printWriter.print("<div class=\"selection_value\">");
                printWriter.print(resultSet.getString("description"));
                printWriter.print("</div>");

                printWriter.print("<hr>");

                String savedCategory = resultSet.getString("category");
                if (savedCategory == null) {
                    savedCategory = "Select Category";
                }
                String categoryLink = "/category/select?transactionId=" + transactionId;
                printWriter.print("<a class=\"plain_link\" href=\"" + categoryLink + "\">");
                printWriter.print("<div class=\"selection_title\">Category</div>");
                printWriter.print("<div class=\"selection_value\">" + savedCategory + "</div>");
                printWriter.print("</a>");

                printWriter.print("<hr>");

                String savedTags = resultSet.getString("tags");
                if (savedTags == null || savedTags.isEmpty()) {
                    savedTags = "Add Tags";
                }
                String tagLink = "/tags/select?transactionId=" + transactionId ;
                printWriter.print("<a class=\"plain_link\" href=\"" + tagLink + "\">");
                printWriter.print("<div class=\"selection_title\">Tags</div>");
                printWriter.print("<div class=\"selection_value\">" + savedTags + "</div>");
                printWriter.print("</a>");

                printWriter.print("<hr>");

                String notes = resultSet.getString("notes");
                if (notes == null) {
                    notes = "Add Notes";
                }
                String notesLink = "/notes/enter?transactionId=" + transactionId;
                printWriter.print("<a class=\"plain_link\" href=\"" + notesLink + "\">");
                printWriter.print("<div class=\"selection_title\">Notes</div>");
                printWriter.print("<div class=\"selection_value\">" + notes + "</div>");
                printWriter.print("</a>");

                printWriter.print("<hr>");
            });
            printWriter.print("</div>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

}
