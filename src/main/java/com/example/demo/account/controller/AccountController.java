package com.example.demo.account.controller;

import com.example.demo.account.dto.request.ChangeAccountStatusRequest;
import com.example.demo.account.dto.request.CreateAccountRequest;
import com.example.demo.account.dto.request.UpdateAccountRequest;
import com.example.demo.account.dto.response.AccountResponse;
import com.example.demo.account.entity.enums.AccountStatus;
import com.example.demo.account.entity.enums.AccountType;
import com.example.demo.account.service.AccountService;
import com.example.demo.accountStatusHistory.dto.response.AccountStatusHistoryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public Page<AccountResponse> search(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String cccd,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) AccountType accountType,
            @PageableDefault(size = 20, sort = "openedAt") Pageable pageable) {
        return accountService.searchAccounts(accountNumber, customerId, cccd, status,
                accountType, pageable);
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @PatchMapping("/{id}/status")
    public AccountStatusHistoryResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeAccountStatusRequest request) {
        return accountService.changeStatus(id, request);
    }

    @GetMapping("/{id}/status-history")
    public List<AccountStatusHistoryResponse> getStatusHistory(@PathVariable Long id) {
        return accountService.getStatusHistory(id);
    }
}