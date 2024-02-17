package com.alfy.budget.controller;

import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/transactions/{transactionId}/tags")
public class TransactionsIdTagsController {

    private final TransactionsService transactionsService;

    @Autowired
    public TransactionsIdTagsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public void showSelection(
            @PathVariable(name = "transactionId") int transactionId,
            HttpServletResponse response
    ) throws IOException {
        final String postUrl = "/transactions/" + transactionId + "/tags";

        // TODO read this from the database
        String[] tags = {
                "Medical",
                "Car",
                "Home Improvement",
                "Savings",
                "Gift",
        };

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Select Tag</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("</head>");
            printWriter.print("<body>");

            Transaction transaction = transactionsService.getTransaction(transactionId);
            Set<String> tagSet = getTagSet(transaction.tags);

            printWriter.print("<form method=\"POST\" action=\"" + postUrl + "\" enctype=\"application/x-www-form-urlencoded\">");
            printWriter.print("<input type=\"hidden\" name=\"transactionId\" value=\"" + transactionId + "\">");
            for (String tag : tags) {
                String checked = tagSet.contains(tag) ? "checked" : "";
                printWriter.print("<input type=\"checkbox\" name=\"tag\" value=\"" + tag + "\" " + checked + ">" + tag + "</input>");
                printWriter.print("<br>");
                printWriter.print("<br>");
            }
            printWriter.print("<input type=\"submit\">");
            printWriter.print("</form>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

    @PostMapping
    public void updateTags(
            @PathVariable(name = "transactionId") int transactionId,
            @RequestParam(name = "tag", required = false) List<String> tags,
            HttpServletResponse response
    ) throws IOException {
        String tagsValue = tags == null ? null : String.join(", ", tags);
        transactionsService.updateTags(transactionId, tagsValue);
        response.sendRedirect("/transactions/" + transactionId);
    }

    private static Set<String> getTagSet(String selectedTags) {
        Set<String> tagSet = new HashSet<>();

        if (selectedTags != null) {
            String[] tags = selectedTags.split(",");
            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    tagSet.add(trimmedTag);
                }
            }
        }

        return tagSet;
    }

}
