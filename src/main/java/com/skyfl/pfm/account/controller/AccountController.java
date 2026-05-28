package com.skyfl.pfm.account.controller;

import com.skyfl.pfm.account.dto.AccountRequest;
import com.skyfl.pfm.account.dto.AccountResponse;
import com.skyfl.pfm.account.service.AccountService;
import com.skyfl.pfm.common.security.CurrentUserResolver;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> getAccounts() {
        return accountService.getAccounts(CurrentUserResolver.get().getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(CurrentUserResolver.get().getId(), request);
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        return accountService.getAccount(CurrentUserResolver.get().getId(), id);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        return accountService.update(CurrentUserResolver.get().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        accountService.archive(CurrentUserResolver.get().getId(), id);
    }

    @GetMapping("/{id}/balance")
    public Map<String, BigDecimal> getBalance(@PathVariable UUID id) {
        return Map.of("balance", accountService.getBalance(CurrentUserResolver.get().getId(), id));
    }
}
