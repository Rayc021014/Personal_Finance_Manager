package com.skyfl.pfm.transaction.service;

import com.skyfl.pfm.account.entity.Account;
import com.skyfl.pfm.account.service.AccountService;
import com.skyfl.pfm.category.entity.Category;
import com.skyfl.pfm.category.service.CategoryService;
import com.skyfl.pfm.common.exception.BusinessException;
import com.skyfl.pfm.transaction.dto.TransactionResponse;
import com.skyfl.pfm.transaction.dto.TransferRequest;
import com.skyfl.pfm.transaction.entity.Transaction;
import com.skyfl.pfm.transaction.entity.TransactionType;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public TransferService(UserRepository userRepository, AccountService accountService,
            CategoryService categoryService, TransactionRepository transactionRepository,
            TransactionService transactionService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public TransactionResponse transfer(UUID userId, TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Transfer accounts must be different");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        Account from = accountService.getEntity(userId, request.fromAccountId());
        Account to = accountService.getEntity(userId, request.toAccountId());
        Category category = categoryService.getEntity(userId, request.categoryId());

        Transaction debit = buildTransfer(user, from, category, request, "Transfer out");
        Transaction credit = buildTransfer(user, to, category, request, "Transfer in");
        transactionRepository.save(debit);
        credit.setTransferPair(debit);
        transactionRepository.save(credit);
        debit.setTransferPair(credit);
        return transactionService.toResponse(debit);
    }

    private Transaction buildTransfer(User user, Account account, Category category, TransferRequest request, String prefix) {
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAccount(account);
        tx.setCategory(category);
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(request.amount());
        tx.setCurrency(account.getCurrency());
        tx.setTransactionDate(request.transactionDate());
        tx.setNote(prefix + (request.note() == null || request.note().isBlank() ? "" : ": " + request.note()));
        return tx;
    }
}
