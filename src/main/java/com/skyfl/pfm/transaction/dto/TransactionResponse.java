package com.skyfl.pfm.transaction.dto;

import com.skyfl.pfm.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        UUID accountId,
        String accountName,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        String currency,
        LocalDate transactionDate,
        String note,
        UUID transferPairId
) {
}
