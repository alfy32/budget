package com.alfy.budget.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@RestController
@RequestMapping(path = "/transactions/{transactionId}/tags")
public class TransactionsIdTagsController {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public TransactionsIdTagsController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
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

            String query = "SELECT tags FROM transactions" +
                    " WHERE id=:id";
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("id", transactionId);
            String selectedTags = namedParameterJdbcTemplate.queryForObject(query, paramMap, String.class);
            Set<String> tagSet = getTagSet(selectedTags);

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

        String s = "UPDATE transactions"
                + " SET tags = :tags"
                + " WHERE id = :id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", transactionId);
        paramMap.put("tags", tagsValue);
        namedParameterJdbcTemplate.update(s, paramMap);

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
