package com.alfy.budget.controller;

import com.alfy.budget.model.Account;
import com.alfy.budget.model.Category;
import com.alfy.budget.service.AccountsService;
import com.alfy.budget.service.CategoriesService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class AccountsController {

    private final AccountsService accountsService;
    private final CategoriesService categoriesService;

    public AccountsController(AccountsService accountsService, CategoriesService categoriesService) {
        this.accountsService = accountsService;
        this.categoriesService = categoriesService;
    }

    @GetMapping("/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", accountsService.list());
        return "accounts";
    }

    @PostMapping("/accounts")
    public String create(@RequestParam String name) {
        UUID id = accountsService.add(name);
        return id == null ? "redirect:/accounts" : "redirect:/accounts/" + id;
    }

    @GetMapping("/accounts/{id}")
    public String account(@PathVariable UUID id, Model model) {
        Map<UUID, String> accountNames = new HashMap<>();
        for (Account account : accountsService.list()) {
            accountNames.put(account.id, account.name);
        }

        List<Category> categories = categoriesService.list();
        Map<UUID, String> categoryNames = new HashMap<>();
        for (Category category : categories) {
            categoryNames.put(category.id, category.name);
        }

        model.addAttribute("account", accountsService.get(id));
        model.addAttribute("history", accountsService.history(id));
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("accountNames", accountNames);
        return "account";
    }

    @PostMapping("/accounts/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setName(@PathVariable UUID id, @RequestParam String name) {
        accountsService.setName(id, name);
    }

    @PostMapping("/accounts/{id}/categories")
    public String setCategories(@PathVariable UUID id, @RequestParam(name = "categoryId", required = false) List<UUID> categoryIds) {
        accountsService.setCategories(id, categoryIds == null ? List.of() : categoryIds);
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/accounts/{id}/delete")
    public String delete(@PathVariable UUID id) {
        accountsService.delete(id);
        return "redirect:/accounts";
    }

}
