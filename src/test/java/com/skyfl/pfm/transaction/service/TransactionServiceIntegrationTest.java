package com.skyfl.pfm.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skyfl.pfm.account.entity.Account;
import com.skyfl.pfm.account.entity.AccountType;
import com.skyfl.pfm.account.repository.AccountRepository;
import com.skyfl.pfm.category.entity.Category;
import com.skyfl.pfm.category.entity.CategoryType;
import com.skyfl.pfm.category.repository.CategoryRepository;
import com.skyfl.pfm.transaction.dto.TransactionResponse;
import com.skyfl.pfm.transaction.entity.Transaction;
import com.skyfl.pfm.transaction.entity.TransactionType;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void searchAppliesDateFiltersWithoutJdbcTypeErrors() {
        User user = new User();
        user.setEmail("ledger@example.com");
        user.setPasswordHash("hash");
        user.setDisplayName("Ledger User");
        user.setCurrency("TWD");
        user = userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setName("Cash");
        account.setType(AccountType.CASH);
        account.setCurrency("TWD");
        account.setInitialBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        Category category = new Category();
        category.setUser(user);
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);
        category.setColor("#E59B52");
        category = categoryRepository.save(category);

        Transaction earlier = createTransaction(user, account, category, LocalDate.of(2026, 5, 10), "Earlier expense");
        Transaction later = createTransaction(user, account, category, LocalDate.of(2026, 5, 20), "Later expense");
        transactionRepository.saveAll(List.of(earlier, later));

        Page<TransactionResponse> fromMidMonth = transactionService.search(
                user.getId(),
                null,
                null,
                null,
                LocalDate.of(2026, 5, 15),
                null,
                null,
                PageRequest.of(0, 10));

        Page<TransactionResponse> untilMidMonth = transactionService.search(
                user.getId(),
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 15),
                null,
                PageRequest.of(0, 10));

        assertThat(fromMidMonth.getContent())
                .extracting(TransactionResponse::note)
                .containsExactly("Later expense");

        assertThat(untilMidMonth.getContent())
                .extracting(TransactionResponse::note)
                .containsExactly("Earlier expense");
    }

    private Transaction createTransaction(User user, Account account, Category category, LocalDate transactionDate, String note) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("250.00"));
        transaction.setCurrency("TWD");
        transaction.setTransactionDate(transactionDate);
        transaction.setNote(note);
        return transaction;
    }
}
