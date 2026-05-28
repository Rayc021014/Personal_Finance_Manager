package com.skyfl.pfm.budget.controller;

import com.skyfl.pfm.budget.dto.BudgetRequest;
import com.skyfl.pfm.budget.dto.BudgetResponse;
import com.skyfl.pfm.budget.dto.BudgetStatusResponse;
import com.skyfl.pfm.budget.service.BudgetService;
import com.skyfl.pfm.common.security.CurrentUserResolver;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> list(@RequestParam short year, @RequestParam short month) {
        return budgetService.list(CurrentUserResolver.get().getId(), year, month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) {
        return budgetService.create(CurrentUserResolver.get().getId(), request);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable UUID id, @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(CurrentUserResolver.get().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        budgetService.delete(CurrentUserResolver.get().getId(), id);
    }

    @GetMapping("/status")
    public List<BudgetStatusResponse> status(@RequestParam short year, @RequestParam short month) {
        return budgetService.status(CurrentUserResolver.get().getId(), year, month);
    }

    @PostMapping("/copy")
    public List<BudgetResponse> copy(@RequestParam String from, @RequestParam String to) {
        return budgetService.copy(CurrentUserResolver.get().getId(), from, to);
    }
}
