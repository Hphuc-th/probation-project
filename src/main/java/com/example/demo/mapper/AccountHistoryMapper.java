package com.example.demo.mapper;

import com.example.demo.dto.response.AccountStatusHistoryResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountStatusHistory;
import com.example.demo.entity.enums.AccountStatus;
import org.springframework.stereotype.Component;

@Component
public class AccountHistoryMapper {

    public AccountStatusHistoryResponse toResponse(AccountStatusHistory history) {
        return AccountStatusHistoryResponse.builder()
                .id(history.getId())
                .accountId(history.getAccount() != null ? history.getAccount().getId() : null)
                .previousStatus(history.getPreviousStatus() != null ? history.getPreviousStatus().name() : null)
                .newStatus(history.getNewStatus() != null ? history.getNewStatus().name() : null)
                .reason(history.getReason())
                .changedAt(history.getChangedAt())
                .build();
    }

    public AccountStatusHistory toEntity(Account account, AccountStatus previousStatus,
                                          AccountStatus newStatus, String reason) {
        AccountStatusHistory history = new AccountStatusHistory();
        history.setAccount(account);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        return history;
    }
}