package com.skyfl.pfm.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skyfl.pfm.account.entity.Account;
import com.skyfl.pfm.account.entity.AccountType;
import com.skyfl.pfm.account.repository.AccountRepository;
import com.skyfl.pfm.category.entity.Category;
import com.skyfl.pfm.category.entity.CategoryType;
import com.skyfl.pfm.category.repository.CategoryRepository;
import com.skyfl.pfm.transaction.dto.TransactionImportResponse;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CsvImportServiceIntegrationTest {

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void importCsvUsesSelectedAccountAndCategoryNames() throws Exception {
        User user = new User();
        user.setEmail("importer@example.com");
        user.setPasswordHash("hash");
        user.setDisplayName("Importer");
        user.setCurrency("TWD");
        user = userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setName("Wallet");
        account.setType(AccountType.CASH);
        account.setCurrency("TWD");
        account.setInitialBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        Category breakfast = new Category();
        breakfast.setUser(null);
        breakfast.setName("早餐");
        breakfast.setType(CategoryType.EXPENSE);
        breakfast.setSystem(true);
        breakfast.setDeleted(false);
        categoryRepository.save(breakfast);

        Category salary = new Category();
        salary.setUser(null);
        salary.setName("薪資");
        salary.setType(CategoryType.INCOME);
        salary.setSystem(true);
        salary.setDeleted(false);
        categoryRepository.save(salary);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                """
                type,categoryName,amount,transactionDate,note
                EXPENSE,早餐,80,2026-05-01,早餐店
                INCOME,薪資,30000,2026-05-02,薪資入帳
                """.getBytes(StandardCharsets.UTF_8)
        );

        TransactionImportResponse response = csvImportService.importCsv(user.getId(), account.getId(), file);

        assertThat(response.successCount()).isEqualTo(2);
        assertThat(response.failedCount()).isZero();
        var userId = user.getId();
        var accountId = account.getId();

        assertThat(transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getUser().getId().equals(userId))
                .toList())
                .hasSize(2)
                .allMatch(transaction -> transaction.getAccount().getId().equals(accountId));
    }
}
