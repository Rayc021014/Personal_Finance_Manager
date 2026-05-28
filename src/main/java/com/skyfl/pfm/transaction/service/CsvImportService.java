package com.skyfl.pfm.transaction.service;

import com.skyfl.pfm.transaction.dto.TransactionImportResponse;
import com.skyfl.pfm.transaction.dto.TransactionRequest;
import com.skyfl.pfm.category.entity.CategoryType;
import com.skyfl.pfm.category.service.CategoryService;
import com.skyfl.pfm.transaction.entity.TransactionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvImportService {

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    public CsvImportService(TransactionService transactionService, CategoryService categoryService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
    }

    public TransactionImportResponse importCsv(UUID userId, UUID accountId, MultipartFile file) throws IOException {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.toLowerCase().contains("type")) {
                    continue;
                }
                try {
                    String[] parts = line.split(",", -1);
                    TransactionType transactionType = TransactionType.valueOf(parts[0].trim().toUpperCase());
                    if (transactionType == TransactionType.TRANSFER) {
                        throw new IllegalArgumentException("TRANSFER rows are not supported by CSV import");
                    }
                    String categoryName = parts[1].trim();
                    TransactionRequest request = new TransactionRequest(
                            transactionType,
                            accountId,
                            categoryService.getEntityByName(
                                    userId,
                                    categoryName,
                                    transactionType == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE
                            ).getId(),
                            new BigDecimal(parts[2].trim()),
                            LocalDate.parse(parts[3].trim()),
                            parts.length > 4 ? parts[4].trim() : null
                    );
                    transactionService.create(userId, request);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    errors.add("Line " + lineNumber + ": " + ex.getMessage());
                }
            }
        }
        return new TransactionImportResponse(success, failed, errors);
    }
}
