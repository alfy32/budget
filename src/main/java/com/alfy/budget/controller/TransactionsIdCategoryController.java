package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/transactions/{transactionId}/category")
public class TransactionsIdCategoryController {

    private final TransactionsService transactionsService;
    private final CategoriesService categoriesService;

    @Autowired
    public TransactionsIdCategoryController(
            TransactionsService transactionsService,
            CategoriesService categoriesService
    ) {
        this.transactionsService = transactionsService;
        this.categoriesService = categoriesService;
    }

    @GetMapping
    public void showSelection(
            @PathVariable(name = "transactionId") String transactionId,
            @RequestParam(name = "needsCategorized", required = false) boolean needsCategorized,
            HttpServletResponse response
    ) throws IOException {

        List<Category> categories = categoriesService.getCategories();
        categories.sort(Comparator.comparing(category -> category.name));

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Select Category</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/transactionCategory.css?cache-buster=" + UUID.randomUUID() + "\">");
            printWriter.print("</head>");
            printWriter.print("<body>");

            String postUrl = "/transactions/" + transactionId + "/category?needsCategorized=" + needsCategorized;
            printWriter.print("<form method=\"POST\" action=\"" + postUrl + "\" enctype=\"application/x-www-form-urlencoded\">");
            for (Category category : categories) {
                printWriter.print("<input type=\"submit\" name=\"category\" value=\"" + category.name + "\">");
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
            @RequestParam(name = "needsCategorized", required = false) boolean needsCategorized,
            HttpServletResponse response
    ) throws IOException {
        if (category != null && !category.isEmpty()) {
            transactionsService.updateCategory(transactionId, category);
        }
        response.sendRedirect("/transactions/" + transactionId + "?needsCategorized=" + needsCategorized);
    }

}
