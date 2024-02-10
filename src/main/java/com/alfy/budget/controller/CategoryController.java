package com.alfy.budget.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Random;

@RestController
@RequestMapping(path = "/category")
public class CategoryController {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public CategoryController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping(path = "/select")
    public void selectCategory(
            @RequestParam(name = "transactionId") String transactionId,
            HttpServletResponse response
    ) throws IOException {

        // TODO read this from the database
        String[] categories = {
                "Eat Out",
                "Gas",
                "Utilities",
                "Vacation",
                "Other Regular",
        };

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Select Category</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/transactions.css?cache-buster=" + new Random().nextInt() + "\">");
            printWriter.print("</head>");
            printWriter.print("<body>");

            String postUrl = "/category/update";
            printWriter.print("<form method=\"POST\" action=\"" + postUrl + "\" enctype=\"application/x-www-form-urlencoded\">");
            printWriter.print("<input type=\"hidden\" name=\"transactionId\" value=\"" + transactionId + "\">");
            for (String category : categories) {
                printWriter.print("<input type=\"submit\" name=\"category\" value=\"" + category + "\">");
                printWriter.print("<br>");
                printWriter.print("<br>");
            }
            printWriter.print("</form>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

    @PostMapping(path = "/update")
    public void updateCategory(
            @RequestParam(name = "transactionId") int transactionId,
            @RequestParam(name = "category", required = false) String category,
            HttpServletResponse response
    ) throws IOException {
        if (category != null && !category.isEmpty()) {
            String s = "UPDATE transactions"
                    + " SET category = :category"
                    + " WHERE id = :id";

            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("id", transactionId);
            paramMap.put("category", category);
            namedParameterJdbcTemplate.update(s, paramMap);
        }

        response.sendRedirect("/transactions/" + transactionId);
    }

}
