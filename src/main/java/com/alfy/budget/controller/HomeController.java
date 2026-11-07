package com.alfy.budget.controller;

import com.alfy.budget.service.AccountsService;
import com.alfy.budget.service.PlansService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final TransactionsService transactionsService;
    private final AccountsService accountsService;
    private final PlansService plansService;

    public HomeController(TransactionsService transactionsService, AccountsService accountsService, PlansService plansService) {
        this.transactionsService = transactionsService;
        this.accountsService = accountsService;
        this.plansService = plansService;
    }

    @GetMapping("/")
    public String home(Model model) {
        plansService.syncCurrentPlan(); // in case a new month started a plan set up ahead of time
        model.addAttribute("needsCategorized", transactionsService.countNeedsCategorized());
        model.addAttribute("transfers", transactionsService.transferSummary());
        model.addAttribute("accounts", accountsService.list());
        return "home";
    }

}
