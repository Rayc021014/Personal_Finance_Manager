package com.skyfl.pfm.transaction.dto;

import java.util.List;

public record TransactionImportResponse(
        int successCount,
        int failedCount,
        List<String> errors
) {
}
