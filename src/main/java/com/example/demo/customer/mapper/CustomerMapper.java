package com.example.demo.customer.mapper;

import com.example.demo.authentication.dto.request.RegisterRequest;
import com.example.demo.customer.dto.request.CreateCustomerRequest;
import com.example.demo.customer.dto.request.UpdateCustomerRequest;
import com.example.demo.customer.dto.response.CustomerResponse;
import com.example.demo.customer.entity.Customer;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "accounts", ignore = true)
    Customer toEntity(CreateCustomerRequest request);
    Customer toEntity(RegisterRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer customer);
}