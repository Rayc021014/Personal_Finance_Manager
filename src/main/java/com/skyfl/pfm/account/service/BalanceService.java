package com.skyfl.pfm.account.service;

import com.skyfl.pfm.account.entity.Account;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

    private final TransactionRepository transactionRepository;

    public BalanceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public BigDecimal calculateBalance(Account account) {
        BigDecimal movement = transactionRepository.calculateNetAmount(account.getUser().getId(), account.getId());
        return account.getInitialBalance().add(movement == null ? BigDecimal.ZERO : movement);
    }

    public Map<UUID, BigDecimal> calculateBalances(List<Account> accounts) {
        return accounts.stream().collect(Collectors.toMap(Account::getId, this::calculateBalance));
    }
}
