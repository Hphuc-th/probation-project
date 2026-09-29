package com.example.demo.recurringTransaction.mapper;

import com.example.demo.account.entity.Account;
import com.example.demo.recurringTransaction.dto.request.CreateRecurringTransactionRequest;
import com.example.demo.recurringTransaction.dto.request.UpdateRecurringTransactionRequest;
import com.example.demo.recurringTransaction.dto.response.RecurringTransactionResponse;
import com.example.demo.recurringTransaction.entity.RecurringTransaction;

import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionMapper {

    public RecurringTransactionResponse toResponse(RecurringTransaction rt) {
        return RecurringTransactionResponse.builder()
                .id(rt.getId())
                .accountNumber(rt.getAccount() != null ? rt.getAccount().getAccountNumber() : null)
                .toAccountNumber(rt.getToAccountId() != null ? rt.getToAccountId().toString() : null)
                .transactionType(rt.getTransactionType())
                .amount(rt.getAmount())
                .fee(rt.getFee())
                .location(rt.getLocation())
                .cronExpression(rt.getCronExpression())
                .status(rt.getStatus())
                .nextRunAt(rt.getNextRunAt())
                .lastRunAt(rt.getLastRunAt())
                .lastError(rt.getLastError())
                .consecutiveFailures(rt.getConsecutiveFailures())
                .createdAt(rt.getCreatedAt())
                .build();
    }

    public RecurringTransaction toEntity(CreateRecurringTransactionRequest request, Account account, Account toAccount) {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setAccount(account);
        rt.setToAccountId(toAccount != null ? toAccount.getId() : null);
        rt.setTransactionType(request.getTransactionType());
        rt.setAmount(request.getAmount());
        rt.setFee(request.getFee() != null ? request.getFee() : java.math.BigDecimal.ZERO);
        rt.setLocation(request.getLocation());
        rt.setCronExpression(request.getCronExpression());
        rt.setStatus(com.example.demo.recurringTransaction.entity.enums.RecurringTransactionStatus.ACTIVE);
        rt.computeNextRun();
        return rt;
    }

    public void updateEntity(UpdateRecurringTransactionRequest request, RecurringTransaction rt, Account toAccount) {
        if (request.getAmount() != null) {
            rt.setAmount(request.getAmount());
        }
        if (request.getFee() != null) {
            rt.setFee(request.getFee());
        }
        if (request.getLocation() != null) {
            rt.setLocation(request.getLocation());
        }
        if (request.getCronExpression() != null) {
            rt.setCronExpression(request.getCronExpression());
            rt.computeNextRun();
        }
        if (request.getToAccountId() != null) {
            rt.setToAccountId(toAccount != null ? toAccount.getId() : request.getToAccountId());
        }
    }
}