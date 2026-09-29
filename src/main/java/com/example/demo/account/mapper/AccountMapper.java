package com.example.demo.account.mapper;

import com.example.demo.account.dto.request.CreateAccountRequest;
import com.example.demo.account.dto.response.AccountResponse;
import com.example.demo.account.entity.Account;
import com.example.demo.account.entity.enums.AccountStatus;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "status", source = "status")
    AccountResponse toResponse(Account account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Account toEntity(CreateAccountRequest request);

    @AfterMapping
    default void setDefaults(CreateAccountRequest request, @MappingTarget Account account) {
        account.setStatus(AccountStatus.ACTIVE);
        account.setOpenedAt(java.time.LocalDateTime.now());
    }
}