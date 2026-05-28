package com.skyfl.pfm.report.dto;

import java.math.BigDecimal;

public record SummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings,
        BigDecimal savingsRate
) {
}
