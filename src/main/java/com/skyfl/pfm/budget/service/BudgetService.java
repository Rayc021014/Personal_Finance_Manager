package com.skyfl.pfm.budget.service;

import com.skyfl.pfm.budget.dto.BudgetRequest;
import com.skyfl.pfm.budget.dto.BudgetResponse;
import com.skyfl.pfm.budget.dto.BudgetStatusResponse;
import com.skyfl.pfm.budget.entity.Budget;
import com.skyfl.pfm.budget.repository.BudgetRepository;
import com.skyfl.pfm.category.entity.Category;
import com.skyfl.pfm.category.service.CategoryService;
import com.skyfl.pfm.common.exception.BusinessException;
import com.skyfl.pfm.common.util.DateUtils;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository,
            CategoryService categoryService, TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID userId, short year, short month) {
        return budgetRepository.findByUserIdAndYearAndMonthOrderByCreatedAtAsc(userId, year, month)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(userId, request.categoryId(), request.year(), request.month())
                .ifPresent(existing -> {
                    throw new BusinessException(HttpStatus.CONFLICT, "Budget already exists for category and month");
                });
        Budget budget = new Budget();
        budget.setUser(getUser(userId));
        apply(userId, budget, request);
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID budgetId, BudgetRequest request) {
        Budget budget = getEntity(userId, budgetId);
        apply(userId, budget, request);
        return toResponse(budget);
    }

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        budgetRepository.delete(getEntity(userId, budgetId));
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> status(UUID userId, short year, short month) {
        List<BudgetStatusResponse> results = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserIdAndYearAndMonthOrderByCreatedAtAsc(userId, year, month)) {
            BigDecimal spent = transactionRepository.sumExpenseByCategory(
                    userId,
                    budget.getCategory().getId(),
                    DateUtils.monthStart(year, month),
                    DateUtils.monthEnd(year, month)
            );
            BigDecimal usage = spent.divide(budget.getAmountLimit(), 4, RoundingMode.HALF_UP);
            String status = usage.compareTo(BigDecimal.ONE) >= 0 ? "EXCEEDED"
                    : usage.compareTo(new BigDecimal("0.8")) >= 0 ? "WARNING" : "SAFE";
            results.add(new BudgetStatusResponse(
                    budget.getId(),
                    budget.getCategory().getId(),
                    budget.getCategory().getName(),
                    budget.getAmountLimit(),
                    spent,
                    usage,
                    status
            ));
        }
        return results;
    }

    @Transactional
    public List<BudgetResponse> copy(UUID userId, String from, String to) {
        short fromYear = Short.parseShort(from.substring(0, 4));
        short fromMonth = Short.parseShort(from.substring(5, 7));
        short toYear = Short.parseShort(to.substring(0, 4));
        short toMonth = Short.parseShort(to.substring(5, 7));
        List<BudgetResponse> created = new ArrayList<>();
        for (Budget source : budgetRepository.findByUserIdAndYearAndMonthOrderByCreatedAtAsc(userId, fromYear, fromMonth)) {
            if (budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(userId, source.getCategory().getId(), toYear, toMonth).isPresent()) {
                continue;
            }
            Budget target = new Budget();
            target.setUser(source.getUser());
            target.setCategory(source.getCategory());
            target.setYear(toYear);
            target.setMonth(toMonth);
            target.setAmountLimit(source.getAmountLimit());
            created.add(toResponse(budgetRepository.save(target)));
        }
        return created;
    }

    private Budget getEntity(UUID userId, UUID budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private void apply(UUID userId, Budget budget, BudgetRequest request) {
        Category category = categoryService.getEntity(userId, request.categoryId());
        budget.setCategory(category);
        budget.setYear(request.year());
        budget.setMonth(request.month());
        budget.setAmountLimit(request.amountLimit());
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getYear(),
                budget.getMonth(),
                budget.getAmountLimit()
        );
    }
}
