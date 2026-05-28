package com.skyfl.pfm.budget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID categoryId,
        @Min(2000) @Max(2100) short year,
        @Min(1) @Max(12) short month,
        @NotNull @Positive BigDecimal amountLimit
) {
}
