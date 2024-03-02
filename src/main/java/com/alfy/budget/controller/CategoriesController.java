package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.service.CategoriesService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/rest/categories")
public class CategoriesController {

    private final CategoriesService categoriesService;

    public CategoriesController(CategoriesService categoriesService) {
        this.categoriesService = categoriesService;
    }

    @GetMapping
    public List<Category> getCategories() {
        List<Category> categories = categoriesService.getCategories();
        categories.sort(Comparator.comparing(category -> category.name.toLowerCase()));
        return categories;
    }

    @PostMapping
    public void newCategory(
            @RequestBody Category category
    ) {
        categoriesService.add(category);
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
