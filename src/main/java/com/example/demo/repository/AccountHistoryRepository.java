package com.example.demo.repository;

import com.example.demo.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AccountHistoryRepository extends
        JpaRepository<AccountStatusHistory, Long>, JpaSpecificationExecutor<AccountStatusHistory> {

    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDescIdDesc(Long accountId);
}
