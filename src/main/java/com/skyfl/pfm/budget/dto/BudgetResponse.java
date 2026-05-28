package com.skyfl.pfm.budget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        short year,
        short month,
        BigDecimal amountLimit
) {
}
