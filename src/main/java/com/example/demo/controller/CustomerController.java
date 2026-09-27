package com.example.demo.controller;

import com.example.demo.dto.request.customer.CreateCustomerRequest;
import com.example.demo.dto.request.customer.UpdateCustomerRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CustomerResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.entity.enums.CustomerStatus;
import com.example.demo.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> search(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) CustomerStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<CustomerResponse> page = customerService.searchCustomers(fullName, email, location, status, pageable);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getCustomerById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.created(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.ok(customerService.updateCustomer(id, request));
    }
}