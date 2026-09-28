package com.example.demo.controller;

import com.example.demo.dto.response.AccountTransactionStatsResponse;
import com.example.demo.dto.response.LocationStatResponse;
import com.example.demo.service.CustomerService;
import com.example.demo.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final CustomerService customerService;

    @GetMapping("/accounts-transactions")
    public AccountTransactionStatsResponse getAccountTransactionStats() {
        return statsService.getAccountTransactionStats();
    }

    @GetMapping("/customers-by-location")
    public List<LocationStatResponse> getCustomersByLocation() {
        return customerService.getCustomersByLocation();
    }
}