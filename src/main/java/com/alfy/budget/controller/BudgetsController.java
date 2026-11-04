package com.alfy.budget.controller;

import com.alfy.budget.model.Budget;
import com.alfy.budget.model.Category;
import com.alfy.budget.service.BudgetReportsService;
import com.alfy.budget.service.BudgetsService;
import com.alfy.budget.service.CategoriesService;
import com.alfy.budget.service.TransactionsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Controller
public class BudgetsController {

    private final BudgetsService budgetsService;
    private final CategoriesService categoriesService;
    private final TransactionsService transactionsService;
    private final BudgetReportsService budgetReportsService;

    public BudgetsController(
            BudgetsService budgetsService,
            CategoriesService categoriesService,
            TransactionsService transactionsService,
            BudgetReportsService budgetReportsService
    ) {
        this.budgetsService = budgetsService;
        this.categoriesService = categoriesService;
        this.transactionsService = transactionsService;
        this.budgetReportsService = budgetReportsService;
    }

    // ---- Reports -----------------------------------------------------------------------------

    @GetMapping("/monthly")
    public String monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());

        model.addAttribute("date", date);
        model.addAttribute("start", start);
        model.addAttribute("end", date.with(TemporalAdjusters.lastDayOfMonth()));
        model.addAttribute("previous", start.minusMonths(1));
        model.addAttribute("next", start.plusMonths(1));
        model.addAttribute("isCurrent", start.equals(LocalDate.now().withDayOfMonth(1)));
        model.addAttribute("budgets", budgetReportsService.monthly(date));
        return "monthly";
    }

    @GetMapping("/yearly")
    public String yearly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate start = date.with(TemporalAdjusters.firstDayOfYear());

        model.addAttribute("date", date);
        model.addAttribute("start", start);
        model.addAttribute("end", date.with(TemporalAdjusters.lastDayOfYear()));
        model.addAttribute("previous", start.minusYears(1));
        model.addAttribute("next", start.plusYears(1));
        model.addAttribute("isCurrent", date.getYear() == LocalDate.now().getYear());
        model.addAttribute("budgets", budgetReportsService.yearly(date));
        return "yearly";
    }

    @GetMapping("/needs-transferred")
    public String needsTransferred(Model model) {
        // Only accounts with something to move.
        model.addAttribute("transfers", budgetReportsService.needsTransferred().stream()
                .filter(transfer -> transfer.amount.signum() != 0)
                .toList());
        return "needs-transferred";
    }

    @PostMapping("/needs-transferred/mark")
    public String markTransferred(@RequestParam String account) {
        transactionsService.markTransactionsTransferredForAccount(account);
        return "redirect:/needs-transferred";
    }

    // ---- Budget settings ---------------------------------------------------------------------

    @GetMapping("/budgets")
    public String budgets(Model model) {
        model.addAttribute("budgets", budgetsService.list());
        return "budgets";
    }

    @PostMapping("/budgets")
    public String create(@RequestParam String name) {
        Budget budget = Budget.create(name, BigDecimal.ZERO);
        budgetsService.add(budget);
        return budget.id == null ? "redirect:/budgets" : "redirect:/budgets/" + budget.id;
    }

    @GetMapping("/budgets/{id}")
    public String budget(@PathVariable UUID id, Model model) {
        List<Category> categories = categoriesService.list().stream()
                .filter(category -> category.budget != null && id.equals(category.budget.id))
                .toList();

        model.addAttribute("budget", budgetsService.get(id));
        model.addAttribute("categories", categories);
        return "budget";
    }

    // These save as soon as a field changes, so they return nothing.

    @PostMapping("/budgets/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setName(@PathVariable UUID id, @RequestParam String name) {
        if (!name.isBlank()) {
            budgetsService.setName(id, name);
        }
    }

    @PostMapping("/budgets/{id}/amount")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAmount(@PathVariable UUID id, @RequestParam BigDecimal amount) {
        budgetsService.setAmount(id, amount);
    }

    @PostMapping("/budgets/{id}/transfer-account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTransferAccount(@PathVariable UUID id, @RequestParam(required = false) String transferAccount) {
        budgetsService.setTransferAccount(id, transferAccount);
    }

    @PostMapping("/budgets/{id}/monthly")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setMonthly(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean monthly) {
        budgetsService.setMonthly(id, monthly);
    }

    @PostMapping("/budgets/{id}/delete")
    public String delete(@PathVariable UUID id) {
        budgetsService.delete(id);
        return "redirect:/budgets";
    }

}
