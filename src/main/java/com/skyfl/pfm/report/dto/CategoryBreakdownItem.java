package com.skyfl.pfm.report.dto;

import com.skyfl.pfm.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.util.UUID;

public record CategoryBreakdownItem(
        UUID categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount
) {
}
