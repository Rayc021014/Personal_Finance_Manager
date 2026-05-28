package com.skyfl.pfm.report.dto;

import java.math.BigDecimal;

public record TrendPoint(
        String label,
        BigDecimal income,
        BigDecimal expense
) {
}
