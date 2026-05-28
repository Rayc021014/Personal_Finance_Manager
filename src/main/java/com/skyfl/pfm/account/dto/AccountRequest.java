package com.skyfl.pfm.account.dto;

import com.skyfl.pfm.account.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType type,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @PositiveOrZero BigDecimal initialBalance,
        @Size(max = 1000) String note
) {
}
