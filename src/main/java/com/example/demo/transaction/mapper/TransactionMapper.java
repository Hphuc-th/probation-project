package com.example.demo.transaction.mapper;

import com.example.demo.transaction.dto.request.CreateTransactionRequest;
import com.example.demo.transaction.dto.response.TransactionResponse;
import com.example.demo.transaction.entity.Transaction;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "accountNumber", source = "account.accountNumber")
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fee", defaultValue = "0")
    Transaction toEntity(CreateTransactionRequest request);
}