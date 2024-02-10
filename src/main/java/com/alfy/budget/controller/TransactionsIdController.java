package com.alfy.budget.controller;

import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping(path = "/transactions")
public class TransactionsIdController {

    private static final DateTimeFormatter TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");

    private final TransactionsService transactionsService;

    public TransactionsIdController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
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

            Transaction transaction = transactionsService.getTransaction(transactionId);

            printWriter.print("<a href=\"/transactions\">Back</a>");

            printWriter.print("<div class=\"transaction_amount\">" + transaction.getFormattedAmount() + "</div>");
            printWriter.print("<div class=\"transaction_description\">" + transaction.description + "</div>");
            printWriter.print("<div class=\"transaction_date\">" + TRANSACTION_DATE_FORMATTER.format(transaction.date) + "</div>");
            printWriter.print("<div class=\"transaction_account\">" + transaction.account + "</div>");

            printWriter.print("<hr>");

            String descriptionLink = "/transactions/" + transactionId + "/description";
            printWriter.print("<a class=\"plain_link\" href=\"" + descriptionLink + "\">");
            printWriter.print("<div class=\"selection_title\">Description</div>");
            printWriter.print("<div class=\"selection_value\">" + transaction.description + "</div>");
            printWriter.print("</a>");

            printWriter.print("<hr>");

            String savedCategory = transaction.category == null ? "Select Category" : transaction.category;
            String categoryLink = "/transactions/" + transactionId + "/category";
            printWriter.print("<a class=\"plain_link\" href=\"" + categoryLink + "\">");
            printWriter.print("<div class=\"selection_title\">Category</div>");
            printWriter.print("<div class=\"selection_value\">" + savedCategory + "</div>");
            printWriter.print("</a>");

            printWriter.print("<hr>");

            String savedTags = transaction.tags == null || transaction.tags.isEmpty() ? "Add Tags" : transaction.tags;
            String tagLink = "/transactions/" + transactionId + "/tags";
            printWriter.print("<a class=\"plain_link\" href=\"" + tagLink + "\">");
            printWriter.print("<div class=\"selection_title\">Tags</div>");
            printWriter.print("<div class=\"selection_value\">" + savedTags + "</div>");
            printWriter.print("</a>");

            printWriter.print("<hr>");

            String notes = transaction.notes == null ? "Add Notes" : transaction.notes;
            String notesLink = "/transactions/" + transactionId + "/notes";
            printWriter.print("<a class=\"plain_link\" href=\"" + notesLink + "\">");
            printWriter.print("<div class=\"selection_title\">Notes</div>");
            printWriter.print("<div class=\"selection_value\">" + notes + "</div>");
            printWriter.print("</a>");

            printWriter.print("<hr>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

}
