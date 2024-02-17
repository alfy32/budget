package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.service.CategoriesService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/categories")
public class CategoriesController {

    private final CategoriesService categoriesService;

    public CategoriesController(CategoriesService categoriesService) {
        this.categoriesService = categoriesService;
    }

    @GetMapping
    public void show(
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Transaction By Category</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/categories.css?cache-buster=" + UUID.randomUUID() + "\">");
            printWriter.print("</head>");
            printWriter.print("<body>");

            printWriter.print("<a href=\"/\">Back to Main Page</a>");

            printWriter.print("<ul>");
            List<Category> categories = categoriesService.getCategories();
            categories.sort(Comparator.comparing(category -> category.name));
            for (Category category : categories) {
                printWriter.print("<li>");
                printWriter.print("<form method=\"POST\" action=\"/categories/" + category.id + "\" enctype=\"application/x-www-form-urlencoded\">");
                printWriter.print("<input type=\"text\" name=\"name\" value=\"" + category.name + "\"/> ");
                printWriter.print("<input type=\"submit\", value=\"Update Category\"> ");
                printWriter.print("</form>");

                printWriter.print("<form method=\"POST\" action=\"/categories/" + category.id + "/delete\" enctype=\"application/x-www-form-urlencoded\">");
                printWriter.print("<input type=\"submit\", value=\"Delete Category\">");
                printWriter.print("</form>");

                printWriter.print("</li>");
            }
            printWriter.print("</ul>");

            printWriter.print("<form method=\"POST\" action=\"/categories\" enctype=\"application/x-www-form-urlencoded\">");
            printWriter.print("<input type=\"text\" name=\"name\"/> <input type=\"submit\", value=\"Add Category\">");
            printWriter.print("</form>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

    @PostMapping
    public void addCategory(
            @RequestParam(name = "name") String name,
            HttpServletResponse response
    ) throws IOException {
        categoriesService.addCategory(name);
        response.sendRedirect("/categories");
    }

    @PostMapping(path = "/{id}")
    public void updateCategory(
            @PathVariable(name = "id") int id,
            @RequestParam(name = "name") String name,
            HttpServletResponse response
    ) throws IOException {
        Category category = new Category();
        category.id = id;
        category.name = name;
        categoriesService.updateCategory(category);
        response.sendRedirect("/categories");
    }

    @PostMapping(path = "/{id}/delete")
    public void delete(
            @PathVariable(name = "id") int id,
            HttpServletResponse response
    ) throws IOException {
        categoriesService.deleteCategory(id);
        response.sendRedirect("/categories");
    }

}
