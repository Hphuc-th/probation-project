package com.example.demo.mapper;

import com.example.demo.dto.request.auth.RegisterRequest;
import com.example.demo.dto.request.customer.CreateCustomerRequest;
import com.example.demo.dto.request.customer.UpdateCustomerRequest;
import com.example.demo.dto.response.CustomerResponse;
import com.example.demo.entity.Customer;
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