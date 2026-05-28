package com.skyfl.pfm.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        @NotNull UUID categoryId,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate transactionDate,
        @Size(max = 1000) String note
) {
}
