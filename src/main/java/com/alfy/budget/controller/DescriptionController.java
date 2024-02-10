package com.alfy.budget.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/description")
public class DescriptionController {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public DescriptionController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping(path = "/edit")
    public void selectCategory(
            @RequestParam(name = "transactionId") int transactionId,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Edit Description</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("</head>");
            printWriter.print("<body>");

            String query = "SELECT transactions.description, bank_transactions.description AS original_description "
                    + " FROM transactions"
                    + " INNER JOIN bank_transactions "
                    + "    ON transactions.bank_transaction_id = bank_transactions.id"
                    + " WHERE transactions.id=:transactionId";
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("transactionId", transactionId);
            Map<String, Object> stringObjectMap = namedParameterJdbcTemplate.queryForMap(query, paramMap);

            String originalDecription = (String) stringObjectMap.get("original_description");
            if (originalDecription == null) {
                originalDecription = "";
            }

            printWriter.print("<p>Original Description: " + originalDecription + "</p>");

            String decription = (String) stringObjectMap.get("description");
            if (decription == null) {
                decription = "";
            }

            String postUrl = "/description/update";
            printWriter.print("<form method=\"POST\" action=\"" + postUrl + "\" enctype=\"application/x-www-form-urlencoded\">");
            printWriter.print("<input type=\"hidden\" name=\"transactionId\" value=\"" + transactionId + "\"/>");
            printWriter.print("<input type=\"text\" name=\"description\" style=\"width: 100%;\" value=\"" + decription + "\"/>");
            printWriter.print("<br>");
            printWriter.print("<br>");
            printWriter.print("<input type=\"submit\">");
            printWriter.print("</form>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

    @PostMapping(path = "/update")
    public void updateDescription(
            @RequestParam(name = "transactionId") int transactionId,
            @RequestParam(name = "description", required = false) String description,
            HttpServletResponse response
    ) throws IOException {
        if (description == null || description.isEmpty()) {
            description = null;
        }

        String s = "UPDATE transactions"
                + " SET description = :description"
                + " WHERE id = :transactionId";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("transactionId", transactionId);
        paramMap.put("description", description);
        namedParameterJdbcTemplate.update(s, paramMap);

        response.sendRedirect("/transactions/" + transactionId);
    }

}
