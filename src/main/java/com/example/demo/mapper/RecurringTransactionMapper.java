package com.example.demo.mapper;

import com.example.demo.dto.request.recurring.CreateRecurringTransactionRequest;
import com.example.demo.dto.request.recurring.UpdateRecurringTransactionRequest;
import com.example.demo.dto.response.RecurringTransactionResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.RecurringTransaction;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionMapper {

    public RecurringTransactionResponse toResponse(RecurringTransaction rt) {
        return RecurringTransactionResponse.builder()
                .id(rt.getId())
                .accountNumber(rt.getAccount() != null ? rt.getAccount().getAccountNumber() : null)
                .toAccountNumber(rt.getToAccount() != null ? rt.getToAccount().getAccountNumber() : null)
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
        rt.setToAccount(toAccount);
        rt.setTransactionType(request.getTransactionType());
        rt.setAmount(request.getAmount());
        rt.setFee(request.getFee() != null ? request.getFee() : java.math.BigDecimal.ZERO);
        rt.setLocation(request.getLocation());
        rt.setCronExpression(request.getCronExpression());
        rt.setStatus(com.example.demo.entity.enums.RecurringTransactionStatus.ACTIVE);
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
            rt.setToAccount(toAccount);
        }
    }
}