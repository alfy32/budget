package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.service.CategoriesService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping(path = "/transactions/{transactionId}/category")
public class TransactionsIdCategoryController {

    private final CategoriesService categoriesService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public TransactionsIdCategoryController(
            CategoriesService categoriesService,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        this.categoriesService = categoriesService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping
    public void showSelection(
            @PathVariable(name = "transactionId") String transactionId,
            HttpServletResponse response
    ) throws IOException {

        List<Category> categories = categoriesService.getCategories();

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Select Category</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("</head>");
            printWriter.print("<body>");

            String postUrl = "/transactions/" + transactionId + "/category";
            printWriter.print("<form method=\"POST\" action=\"" + postUrl + "\" enctype=\"application/x-www-form-urlencoded\">");
            for (Category category : categories) {
                printWriter.print("<input type=\"submit\" name=\"category\" value=\"" + category.name + "\">");
                printWriter.print("<br>");
                printWriter.print("<br>");
            }
            printWriter.print("</form>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

    @PostMapping
    public void update(
            @PathVariable(name = "transactionId") int transactionId,
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
