package com.skyfl.pfm.account.dto;

import com.skyfl.pfm.account.entity.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        String note
) {
}
