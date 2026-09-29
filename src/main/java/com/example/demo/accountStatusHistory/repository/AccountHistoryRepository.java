package com.example.demo.accountStatusHistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.demo.accountStatusHistory.entity.AccountStatusHistory;

import java.util.List;

public interface AccountHistoryRepository extends
        JpaRepository<AccountStatusHistory, Long>, JpaSpecificationExecutor<AccountStatusHistory> {

    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDescIdDesc(Long accountId);
}
