package com.alfy.budget.controller;

import com.alfy.budget.model.Category;
import com.alfy.budget.service.AccountsService;
import com.alfy.budget.service.AutoCategorizeService;
import com.alfy.budget.service.BudgetsService;
import com.alfy.budget.service.CategoriesService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
public class CategoriesController {

    private final CategoriesService categoriesService;
    private final BudgetsService budgetsService;
    private final AutoCategorizeService autoCategorizeService;
    private final AccountsService accountsService;

    public CategoriesController(
            CategoriesService categoriesService,
            BudgetsService budgetsService,
            AutoCategorizeService autoCategorizeService,
            AccountsService accountsService
    ) {
        this.categoriesService = categoriesService;
        this.budgetsService = budgetsService;
        this.autoCategorizeService = autoCategorizeService;
        this.accountsService = accountsService;
    }

    // ---- Categories --------------------------------------------------------------------------

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoriesService.list());
        return "categories";
    }

    @PostMapping("/categories")
    public String create(@RequestParam String name) {
        Category category = Category.create(name);
        categoriesService.add(category);
        return "redirect:/categories";
    }

    @GetMapping("/categories/{id}")
    public String category(@PathVariable UUID id, Model model) {
        model.addAttribute("category", categoriesService.get(id));
        model.addAttribute("budgets", budgetsService.list());
        model.addAttribute("accounts", accountsService.list());
        return "category";
    }

    @PostMapping("/categories/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setName(@PathVariable UUID id, @RequestParam String name) {
        categoriesService.setName(id, name);
    }

    @PostMapping("/categories/{id}/budget")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBudget(@PathVariable UUID id, @RequestParam(required = false) UUID budgetId) {
        categoriesService.setBudget(id, budgetId);
    }

    @PostMapping("/categories/{id}/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAccount(@PathVariable UUID id, @RequestParam(required = false) UUID accountId) {
        categoriesService.setAccount(id, accountId);
    }

    @PostMapping("/categories/{id}/delete")
    public String delete(@PathVariable UUID id) {
        categoriesService.delete(id);
        return "redirect:/categories";
    }

    // ---- Auto-categorize rules ---------------------------------------------------------------

    @GetMapping("/rules")
    public String rules(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID categoryId,
            Model model
    ) {
        model.addAttribute("rules", autoCategorizeService.list());
        model.addAttribute("categories", categoriesService.list());
        model.addAttribute("description", description);
        model.addAttribute("categoryId", categoryId);
        return "rules";
    }

    @PostMapping("/rules")
    public String createRule(@RequestParam String description, @RequestParam UUID categoryId) {
        autoCategorizeService.add(description, categoryId);
        return "redirect:/rules";
    }

    @PostMapping("/rules/{id}/delete")
    public String deleteRule(@PathVariable UUID id) {
        autoCategorizeService.delete(id);
        return "redirect:/rules";
    }

}
