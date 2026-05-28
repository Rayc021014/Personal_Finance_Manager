package com.skyfl.pfm.budget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetStatusResponse(
        UUID budgetId,
        UUID categoryId,
        String categoryName,
        BigDecimal amountLimit,
        BigDecimal spentAmount,
        BigDecimal usageRate,
        String status
) {
}
