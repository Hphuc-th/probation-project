package com.example.demo.mapper;

import com.example.demo.dto.request.customer.CreateCustomerRequest;
import com.example.demo.dto.request.customer.UpdateCustomerRequest;
import com.example.demo.dto.response.CustomerResponse;
import com.example.demo.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .location(customer.getLocation())
                .cccd(customer.getCccd())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    public Customer toEntity(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setLocation(request.getLocation());
        customer.setCccd(request.getCccd());
        return customer;
    }

    public void updateEntity(UpdateCustomerRequest request, Customer customer) {
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setLocation(request.getLocation());
        if (request.getCccd() != null) {
            customer.setCccd(request.getCccd());
        }
    }
}
