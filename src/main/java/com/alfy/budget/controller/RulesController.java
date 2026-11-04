package com.alfy.budget.controller;

import com.alfy.budget.model.CategorizeRule;
import com.alfy.budget.model.CategorizeRule.MatchType;
import com.alfy.budget.service.AutoCategorizeService;
import com.alfy.budget.service.CategoriesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Auto-categorize rules: list, add, edit (with a live preview of what the rule matches), apply, delete.
 */
@Controller
public class RulesController {

    private final AutoCategorizeService autoCategorizeService;
    private final CategoriesService categoriesService;

    public RulesController(AutoCategorizeService autoCategorizeService, CategoriesService categoriesService) {
        this.autoCategorizeService = autoCategorizeService;
        this.categoriesService = categoriesService;
    }

    @GetMapping("/rules")
    public String rules(Model model) {
        model.addAttribute("rules", autoCategorizeService.list());
        return "rules";
    }

    /** A blank rule. description and categoryId can be filled in, e.g. from a transaction's page. */
    @GetMapping("/rules/new")
    public String newRule(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID categoryId,
            Model model
    ) {
        CategorizeRule rule = new CategorizeRule();
        rule.description = description;
        rule.categoryId = categoryId;
        return form(rule, null, model);
    }

    @PostMapping("/rules")
    public String create(
            @RequestParam String description,
            @RequestParam(defaultValue = "CONTAINS") MatchType matchType,
            @RequestParam UUID categoryId
    ) {
        UUID id = autoCategorizeService.add(description, matchType, categoryId);
        return id == null ? "redirect:/rules" : "redirect:/rules/" + id + "?saved=true";
    }

    @GetMapping("/rules/{id}")
    public String rule(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer applied,
            @RequestParam(defaultValue = "false") boolean saved,
            Model model
    ) {
        model.addAttribute("applied", applied);
        model.addAttribute("saved", saved);
        CategorizeRule rule = autoCategorizeService.get(id);
        return form(rule, autoCategorizeService.preview(rule), model);
    }

    private String form(CategorizeRule rule, AutoCategorizeService.Preview savedPreview, Model model) {
        model.addAttribute("title", rule.id == null ? "New rule" : "Edit rule");
        model.addAttribute("rule", rule);
        model.addAttribute("matchTypes", MatchType.values());
        model.addAttribute("categories", categoriesService.list());
        model.addAttribute("preview", autoCategorizeService.preview(rule));
        // What the saved rule (not unsaved edits) would categorize; drives the Apply button.
        model.addAttribute("savedPreview", savedPreview);
        return "rule";
    }

    @PostMapping("/rules/{id}")
    public String update(
            @PathVariable UUID id,
            @RequestParam String description,
            @RequestParam(defaultValue = "CONTAINS") MatchType matchType,
            @RequestParam UUID categoryId
    ) {
        autoCategorizeService.update(id, description, matchType, categoryId);
        return "redirect:/rules/" + id + "?saved=true";
    }

    /** Live preview while editing: what the rule as typed would match. Returns just that part of the page. */
    @GetMapping("/rules/preview")
    public String preview(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "CONTAINS") MatchType matchType,
            Model model
    ) {
        CategorizeRule rule = new CategorizeRule();
        rule.id = id;
        rule.description = description;
        rule.matchType = matchType;
        model.addAttribute("preview", autoCategorizeService.preview(rule));
        return "rule :: preview";
    }

    @PostMapping("/rules/{id}/apply")
    public String apply(@PathVariable UUID id) {
        int applied = autoCategorizeService.applyToUncategorized(id);
        return "redirect:/rules/" + id + "?applied=" + applied;
    }

    @PostMapping("/rules/{id}/delete")
    public String delete(@PathVariable UUID id) {
        autoCategorizeService.delete(id);
        return "redirect:/rules";
    }

}
