package com.skyfl.pfm.transaction.controller;

import com.skyfl.pfm.common.security.CurrentUserResolver;
import com.skyfl.pfm.transaction.dto.TransactionImportResponse;
import com.skyfl.pfm.transaction.dto.TransactionRequest;
import com.skyfl.pfm.transaction.dto.TransactionResponse;
import com.skyfl.pfm.transaction.dto.TransactionUpdateRequest;
import com.skyfl.pfm.transaction.dto.TransferRequest;
import com.skyfl.pfm.transaction.entity.TransactionType;
import com.skyfl.pfm.transaction.service.CsvImportService;
import com.skyfl.pfm.transaction.service.TransactionService;
import com.skyfl.pfm.transaction.service.TransferService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransferService transferService;
    private final CsvImportService csvImportService;

    public TransactionController(TransactionService transactionService, TransferService transferService,
            CsvImportService csvImportService) {
        this.transactionService = transactionService;
        this.transferService = transferService;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public Page<TransactionResponse> search(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return transactionService.search(CurrentUserResolver.get().getId(), accountId, categoryId, type, startDate, endDate,
                keyword, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @org.springframework.web.bind.annotation.RequestBody TransactionRequest request) {
        return transactionService.create(CurrentUserResolver.get().getId(), request);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return transactionService.get(CurrentUserResolver.get().getId(), id);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody TransactionUpdateRequest request) {
        return transactionService.update(CurrentUserResolver.get().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        transactionService.delete(CurrentUserResolver.get().getId(), id);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@Valid @org.springframework.web.bind.annotation.RequestBody TransferRequest request) {
        return transferService.transfer(CurrentUserResolver.get().getId(), request);
    }

    @PostMapping("/import")
    public TransactionImportResponse importCsv(@RequestParam UUID accountId, @RequestPart("file") MultipartFile file)
            throws IOException {
        return csvImportService.importCsv(CurrentUserResolver.get().getId(), accountId, file);
    }

    @PostMapping("/{id}/attachment")
    public TransactionResponse uploadAttachment(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return transactionService.attachFile(CurrentUserResolver.get().getId(), id, file);
    }
}
