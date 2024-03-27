package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.service.BudgetsService;
import com.alfy.budget.service.CategoriesService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/rest/categories")
public class CategoriesController {

    private final BudgetsService budgetsService;
    private final CategoriesService categoriesService;

    public CategoriesController(
            BudgetsService budgetsService,
            CategoriesService categoriesService
    ) {
        this.budgetsService = budgetsService;
        this.categoriesService = categoriesService;
    }

    @GetMapping
    public List<Category> getCategories() {
        List<Category> categories = categoriesService.list();
        categories.sort(Comparator.comparing(category -> category.name.toLowerCase()));
        return categories;
    }

    @PostMapping
    public void newCategory(
            @RequestBody Category category
    ) {
        categoriesService.add(category);
    }

    @GetMapping(path = "/{id}")
    public Category getCategory(
            @PathVariable(name = "id") UUID id
    ) {
        Category category = categoriesService.get(id);
        if (category.budget != null && category.budget.id != null) {
            category.budget = budgetsService.get(category.budget.id);
        }
        return category;
    }

    @PostMapping(path = "/{id}")
    public void update(
            @PathVariable(name = "id") UUID id,
            @RequestBody Category category
    ) {
        category.id = id;
        categoriesService.update(category);
    }

    @DeleteMapping(path = "/{id}")
    public void deleteCategory(
            @PathVariable(name = "id") UUID id
    ) throws IOException {
        categoriesService.delete(id);
    }

}
