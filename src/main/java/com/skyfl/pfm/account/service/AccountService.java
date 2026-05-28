package com.skyfl.pfm.account.service;

import com.skyfl.pfm.account.dto.AccountRequest;
import com.skyfl.pfm.account.dto.AccountResponse;
import com.skyfl.pfm.account.entity.Account;
import com.skyfl.pfm.account.repository.AccountRepository;
import com.skyfl.pfm.common.exception.BusinessException;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
            BalanceService balanceService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.balanceService = balanceService;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(UUID userId) {
        return accountRepository.findByUserIdAndArchivedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID userId, UUID accountId) {
        return toResponse(getEntity(userId, accountId));
    }

    @Transactional
    public AccountResponse create(UUID userId, AccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        Account account = new Account();
        account.setUser(user);
        apply(account, request, true);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID accountId, AccountRequest request) {
        Account account = getEntity(userId, accountId);
        apply(account, request, false);
        return toResponse(account);
    }

    @Transactional
    public void archive(UUID userId, UUID accountId) {
        Account account = getEntity(userId, accountId);
        account.setArchived(true);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID userId, UUID accountId) {
        return balanceService.calculateBalance(getEntity(userId, accountId));
    }

    public Account getEntity(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserIdAndArchivedFalse(accountId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private void apply(Account account, AccountRequest request, boolean includeInitialBalance) {
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setCurrency(request.currency().trim().toUpperCase());
        account.setNote(request.note());
        if (includeInitialBalance) {
            account.setInitialBalance(request.initialBalance());
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getInitialBalance(),
                balanceService.calculateBalance(account),
                account.getNote()
        );
    }
}
