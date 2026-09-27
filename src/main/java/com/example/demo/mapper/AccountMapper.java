package com.example.demo.mapper;

import com.example.demo.dto.request.account.CreateAccountRequest;
import com.example.demo.dto.response.AccountResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.enums.AccountStatus;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomer() != null ? account.getCustomer().getId() : null)
                .balance(account.getBalance())
                .accountType(account.getAccountType() != null ? account.getAccountType().name() : null)
                .transactionLimit(account.getTransactionLimit())
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .openedAt(account.getOpenedAt())
                .build();
    }

    public Account toEntity(CreateAccountRequest request) {
        Account account = new Account();
        account.setAccountNumber(request.getAccountNumber());
        account.setBalance(request.getBalance());
        account.setAccountType(request.getAccountType());
        account.setTransactionLimit(request.getTransactionLimit());
        account.setStatus(AccountStatus.ACTIVE);
        account.setOpenedAt(java.time.LocalDateTime.now());
        return account;
    }
}
