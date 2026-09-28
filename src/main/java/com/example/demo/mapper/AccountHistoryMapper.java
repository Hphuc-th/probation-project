package com.example.demo.mapper;

import com.example.demo.dto.response.AccountStatusHistoryResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountStatusHistory;
import com.example.demo.entity.enums.AccountStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AccountHistoryMapper {

    @Mapping(target = "accountId", source = "account.id")
    AccountStatusHistoryResponse toResponse(AccountStatusHistory history);

    @Named("toEntity")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    AccountStatusHistory toEntity(Account account, AccountStatus previousStatus, AccountStatus newStatus, String reason);
}