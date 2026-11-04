package com.alfy.budget.controller;

import com.alfy.budget.service.AccountsService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final TransactionsService transactionsService;
    private final AccountsService accountsService;

    public HomeController(TransactionsService transactionsService, AccountsService accountsService) {
        this.transactionsService = transactionsService;
        this.accountsService = accountsService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("needsCategorized", transactionsService.countNeedsCategorized());
        model.addAttribute("needsTransferred", transactionsService.countNeedsTransferred());
        model.addAttribute("accounts", accountsService.list());
        return "home";
    }

}
